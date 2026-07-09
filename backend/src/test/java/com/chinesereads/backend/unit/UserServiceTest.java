package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chinesereads.backend.Model.User;
import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Repository.UserTextRepository;
import com.chinesereads.backend.Service.StripeService;
import com.chinesereads.backend.Service.UserService;
import com.chinesereads.backend.dto.AdminUserDetailDTO;
import com.chinesereads.backend.dto.UserDTO;
import com.chinesereads.backend.dto.UserMapper;
import com.chinesereads.backend.dto.UserMapperImpl;

public class UserServiceTest {

    private UserRepository userRepository;
    private UserTextRepository userTextRepository;
    private UserService userService;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private StripeService stripeService;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        userTextRepository = mock(UserTextRepository.class);
        userMapper = new UserMapperImpl();
        passwordEncoder = new BCryptPasswordEncoder();
        stripeService = mock(StripeService.class);
        userService = new UserService();

        // Inyectamos dependencias manualmente via reflection
        injectField(userService, "userRepository", userRepository);
        injectField(userService, "userTextRepository", userTextRepository);
        injectField(userService, "userMapper", userMapper);
        injectField(userService, "passwordEncoder", passwordEncoder);
        injectField(userService, "stripeService", stripeService);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Admin user detail includes the user's private text count")
    public void testGetUserDetailIncludesTextCount() {
        User user = new User("u@test.com", "U", "encoded", "es", "USER");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userTextRepository.countByOwner(user)).thenReturn(3L);

        AdminUserDetailDTO detail = userService.getUserDetail(1L);

        assertEquals(3, detail.textsCount());
        verify(userTextRepository, times(1)).countByOwner(user);
    }

    // Test unitario 1: Cuando se registra un usuario con un email nuevo, se guarda correctamente
    @Test
    @DisplayName("When a new user is registered with a unique email, it should be saved")
    public void testSaveNewUser() {
        UserDTO userDTO = new UserDTO(null, "test@test.com", "Test User",
                "es", List.of(), List.of("USER"), "password123", null);

        User savedUser = new User("test@test.com", "Test User", "encodedPassword", "es", "USER");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDTO result = userService.save(userDTO);

        assertNotNull(result);
        assertEquals("test@test.com", result.email());
        verify(userRepository, times(1)).save(any(User.class));
    }

    // Test unitario 2: Cuando se registra un usuario con un email ya existente, devuelve null
    @Test
    @DisplayName("When a user is registered with an existing email, it should return null")
    public void testSaveDuplicateEmail() {
        UserDTO userDTO = new UserDTO(null, "existing@test.com", "Test User",
                "es", List.of(), List.of("USER"), "password123", null);

        User existingUser = new User("existing@test.com", "Existing User", "encoded", "es", "USER");
        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(existingUser));

        UserDTO result = userService.save(userDTO);

        assertNull(result);
        verify(userRepository, never()).save(any(User.class));
    }

    // Test unitario 3: Cuando se comprueba una contraseña correcta, devuelve true
    @Test
    @DisplayName("When the correct password is checked, it should return true")
    public void testCheckPasswordCorrect() {
        String rawPassword = "mypassword";
        String encoded = passwordEncoder.encode(rawPassword);
        User user = new User("u@u.com", "User", encoded, "es", "USER");

        when(userRepository.findByEmail("u@u.com")).thenReturn(Optional.of(user));

        boolean result = userService.checkPassword("u@u.com", rawPassword);

        assertTrue(result);
    }

    // Test unitario 4: Cuando se comprueba una contraseña incorrecta, devuelve false
    @Test
    @DisplayName("When the wrong password is checked, it should return false")
    public void testCheckPasswordWrong() {
        String encoded = passwordEncoder.encode("correctpassword");
        User user = new User("u@u.com", "User", encoded, "es", "USER");

        when(userRepository.findByEmail("u@u.com")).thenReturn(Optional.of(user));

        boolean result = userService.checkPassword("u@u.com", "wrongpassword");

        assertFalse(result);
    }

    // Test unitario: un admin puede borrar un usuario con email nulo (dato basura)
    // sin provocar NullPointerException en la salvaguarda anti-auto-borrado.
    @Test
    @DisplayName("Admin can delete a user with a null email without throwing")
    public void testDeleteUserWithNullEmail() {
        User junk = new User(); // email queda a null
        junk.setId(99);

        when(userRepository.findById(99L)).thenReturn(Optional.of(junk));

        userService.deleteUser(99L, "admin@test.com");

        verify(userRepository, times(1)).delete(junk);
    }

    // Revocar premium (premiumUntil = null) debe cancelar también la suscripción de Stripe.
    @Test
    @DisplayName("Revoking premium cancels the Stripe subscription")
    public void testRevokePremiumCancelsSubscription() {
        User user = new User("p@p.com", "P", "encoded", "es", "USER");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        userService.setPremium(5L, null);

        verify(stripeService, times(1)).cancelSubscription(user);
    }

    // Conceder/extender premium NO debe cancelar la suscripción.
    @Test
    @DisplayName("Granting premium does not cancel the Stripe subscription")
    public void testGrantPremiumDoesNotCancel() {
        User user = new User("q@q.com", "Q", "encoded", "es", "USER");
        when(userRepository.findById(6L)).thenReturn(Optional.of(user));

        userService.setPremium(6L, java.time.LocalDateTime.now().plusDays(30));

        verify(stripeService, never()).cancelSubscription(any());
    }

    // Borrar la propia cuenta debe cancelar la suscripción antes de eliminar el registro.
    @Test
    @DisplayName("Deleting own account cancels the Stripe subscription")
    public void testDeleteOwnAccountCancelsSubscription() {
        User user = new User("r@r.com", "R", "encoded", "es", "USER");
        when(userRepository.findByEmail("r@r.com")).thenReturn(Optional.of(user));

        userService.deleteOwnAccount("r@r.com");

        verify(stripeService, times(1)).cancelSubscription(user);
        verify(userRepository, times(1)).delete(user);
    }

    // Test unitario 5: Cuando se actualiza el perfil, se guarda el nuevo nombre y lenguaje
    @Test
    @DisplayName("When a profile is updated, name and language should be saved")
    public void testUpdateProfile() {
        User user = new User("u@u.com", "Old Name", "encoded", "en", "USER");
        UserDTO updateData = new UserDTO(null, "u@u.com", "New Name",
                "es", List.of(), List.of("USER"), null, null);

        when(userRepository.findByEmail("u@u.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = userService.updateProfile("u@u.com", updateData);

        assertEquals("New Name", result.name());
        assertEquals("es", result.language());
        verify(userRepository, times(1)).save(any(User.class));
    }
}