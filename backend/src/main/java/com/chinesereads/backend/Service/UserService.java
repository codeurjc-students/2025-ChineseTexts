package com.chinesereads.backend.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chinesereads.backend.Model.Collection;
import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Repository.UserTextRepository;
import com.chinesereads.backend.dto.AdminCollectionSummaryDTO;
import com.chinesereads.backend.dto.AdminUserDTO;
import com.chinesereads.backend.dto.AdminUserDetailDTO;
import com.chinesereads.backend.dto.AdminUserUpdateDTO;
import com.chinesereads.backend.dto.UserDTO;
import com.chinesereads.backend.dto.UserMapper;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserTextRepository userTextRepository;

    public UserDTO save(UserDTO user) {
        if (userRepository.findByEmail(user.email()).isPresent()) {
            return null;
        } else {
            User newUser = userMapper.toDomain(user);
            newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
            newUser.setRegistrationDate(LocalDate.now());
            // Record proof of consent (GDPR): the signup form requires accepting the
            // terms of use, so reaching registration means consent was given now.
            newUser.setTermsAcceptedAt(LocalDateTime.now());
            return userMapper.toDTO(userRepository.save(newUser));
        }
    }

    /** Records the moment a user successfully authenticated (called from login). */
    public void recordLastAccess(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastAccess(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public UserDTO findByEmail(String email) {
        return userMapper.toDTO(userRepository.findByEmail(email).orElseThrow());
    }

    public UserDTO updateProfile(String email, UserDTO data) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setName(data.name());
        user.setLanguage(data.language());
        return userMapper.toDTO(userRepository.save(user));
    }

    public boolean checkPassword(String email, String rawPassword) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public UserDTO changePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        return userMapper.toDTO(userRepository.save(user));
    }

    // ——————————————————— Self-service account deletion ———————————————————

    /**
     * Permanently deletes the authenticated user's own account. The JPA cascade
     * on {@code User.collections} removes their collections and flashcards.
     */
    @Transactional
    public void deleteOwnAccount(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        userRepository.delete(user);
    }

    // ——————————————————————— Admin user management ———————————————————————

    /** Paginated user list for the admin panel, optionally filtered by email/name. */
    @Transactional(readOnly = true)
    public List<AdminUserDTO> listUsers(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<User> users;
        if (search == null || search.isBlank()) {
            users = userRepository.findAll(pageable);
        } else {
            String term = search.trim();
            users = userRepository
                    .findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(term, term, pageable);
        }
        return users.map(this::toAdminUserDTO).getContent();
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDTO getUserDetail(long id) {
        User user = userRepository.findById(id).orElseThrow();

        List<AdminCollectionSummaryDTO> collections = user.getCollections().stream()
                .map(c -> new AdminCollectionSummaryDTO(
                        c.getId(),
                        c.getTitle(),
                        c.getFlashcards() != null ? c.getFlashcards().size() : 0))
                .toList();

        int flashcardsCount = collections.stream()
                .mapToInt(AdminCollectionSummaryDTO::flashcardsCount).sum();

        int textsCount = (int) userTextRepository.countByOwner(user);

        return new AdminUserDetailDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getLanguage(),
                user.getRoles(),
                user.isBlocked(),
                user.getRegistrationDate(),
                user.getLastAccess(),
                user.getPremiumUntil(),
                collections.size(),
                flashcardsCount,
                textsCount,
                collections);
    }

    /** Blocks or unblocks a user. Admins cannot block their own account. */
    @Transactional
    public AdminUserDTO setBlocked(long id, boolean blocked, String requesterEmail) {
        User user = userRepository.findById(id).orElseThrow();
        if (Objects.equals(user.getEmail(), requesterEmail)) {
            throw new IllegalStateException("You cannot block your own account.");
        }
        user.setBlocked(blocked);
        return toAdminUserDTO(userRepository.save(user));
    }

    /**
     * Grants, extends or revokes a user's PREMIUM subscription from the admin panel.
     * {@code premiumUntil} is the expiry moment chosen by the admin; {@code null}
     * revokes premium (back to the free plan). This is the single source of truth for
     * premium status, so no role is touched here.
     */
    @Transactional
    public AdminUserDetailDTO setPremium(long id, LocalDateTime premiumUntil) {
        User user = userRepository.findById(id).orElseThrow();
        user.setPremiumUntil(premiumUntil);
        userRepository.save(user);
        return getUserDetail(id);
    }

    /**
     * Edits another user's name / language / roles from the admin panel.
     * Null fields are left unchanged. An admin cannot strip ADMIN from themselves
     * (which would lock them out of the panel).
     */
    @Transactional
    public AdminUserDetailDTO updateUserByAdmin(long id, AdminUserUpdateDTO data, String requesterEmail) {
        User user = userRepository.findById(id).orElseThrow();

        if (data.name() != null) {
            user.setName(data.name());
        }
        if (data.language() != null) {
            user.setLanguage(data.language());
        }
        if (data.roles() != null && !data.roles().isEmpty()) {
            if (Objects.equals(user.getEmail(), requesterEmail) && !data.roles().contains("ADMIN")) {
                throw new IllegalStateException("You cannot remove your own admin role.");
            }
            user.setRoles(data.roles());
        }
        userRepository.save(user);
        return getUserDetail(id);
    }

    /** Permanently deletes a user. Admins cannot delete their own account here. */
    @Transactional
    public void deleteUser(long id, String requesterEmail) {
        User user = userRepository.findById(id).orElseThrow();
        if (Objects.equals(user.getEmail(), requesterEmail)) {
            throw new IllegalStateException("You cannot delete your own account from the admin panel.");
        }
        userRepository.delete(user);
    }

    private AdminUserDTO toAdminUserDTO(User user) {
        return new AdminUserDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getLanguage(),
                user.getRoles(),
                user.isBlocked(),
                user.getRegistrationDate(),
                user.getLastAccess(),
                user.getPremiumUntil());
    }
}