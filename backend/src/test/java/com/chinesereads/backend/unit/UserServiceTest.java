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
import com.chinesereads.backend.Service.UserService;
import com.chinesereads.backend.dto.UserDTO;
import com.chinesereads.backend.dto.UserMapper;
import com.chinesereads.backend.dto.UserMapperImpl;

public class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        userMapper = new UserMapperImpl();
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService();

        // Inyectamos dependencias manualmente via reflection
        injectField(userService, "userRepository", userRepository);
        injectField(userService, "userMapper", userMapper);
        injectField(userService, "passwordEncoder", passwordEncoder);
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