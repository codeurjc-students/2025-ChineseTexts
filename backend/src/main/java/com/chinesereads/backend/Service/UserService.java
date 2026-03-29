package com.chinesereads.backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
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

    public UserDTO save(UserDTO user) {
        if (userRepository.findByEmail(user.email()).isPresent()) {
            return null;
        } else {
            User newUser = userMapper.toDomain(user);
            newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
            return userMapper.toDTO(userRepository.save(newUser));
        }
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
}