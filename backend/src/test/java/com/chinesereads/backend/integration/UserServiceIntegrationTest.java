package com.chinesereads.backend.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.chinesereads.backend.Repository.UserRepository;
import com.chinesereads.backend.Service.UserService;
import com.chinesereads.backend.dto.UserDTO;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
    }

    // Test de integración 1: Cuando se registra un usuario, se persiste en la base de datos real
    @Test
    @DisplayName("When a user is registered, it should be persisted in the database")
    public void testSaveUserPersisted() {
        UserDTO userDTO = new UserDTO(null, "integration@test.com", "Integration User",
                "es", List.of(), List.of("USER"), "password123", null);

        UserDTO saved = userService.save(userDTO);

        assertNotNull(saved);
        assertNotNull(saved.id());
        assertTrue(userRepository.findByEmail("integration@test.com").isPresent());
    }

    // Test de integración 2: Cuando se registra un usuario duplicado, devuelve null y no crea duplicado
    @Test
    @DisplayName("When a duplicate user is registered, it should return null")
    public void testSaveDuplicateUserReturnsNull() {
        UserDTO userDTO = new UserDTO(null, "dup@test.com", "User",
                "es", List.of(), List.of("USER"), "password123", null);

        userService.save(userDTO);
        UserDTO duplicate = userService.save(userDTO);

        assertNull(duplicate);
        assertEquals(1, userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals("dup@test.com")).count());
    }

    // Test de integración 3: Cuando se actualiza el perfil, los cambios se persisten en la base de datos
    @Test
    @DisplayName("When a profile is updated, changes should be persisted")
    public void testUpdateProfilePersisted() {
        UserDTO userDTO = new UserDTO(null, "update@test.com", "Old Name",
                "en", List.of(), List.of("USER"), "password123", null);
        userService.save(userDTO);

        UserDTO updateData = new UserDTO(null, "update@test.com", "New Name",
                "es", List.of(), List.of("USER"), null, null);
        userService.updateProfile("update@test.com", updateData);

        var updated = userRepository.findByEmail("update@test.com").orElseThrow();
        assertEquals("New Name", updated.getName());
        assertEquals("es", updated.getLanguage());
    }

    // Test de integración 4: La contraseña se guarda encriptada en la base de datos
    @Test
    @DisplayName("When a user is saved, the password should be stored encrypted")
    public void testPasswordIsEncrypted() {
        UserDTO userDTO = new UserDTO(null, "enc@test.com", "User",
                "es", List.of(), List.of("USER"), "plainpassword", null);
        userService.save(userDTO);

        var saved = userRepository.findByEmail("enc@test.com").orElseThrow();
        assertTrue(saved.getPassword().startsWith("$2a$"));
    }

    // Test de integración 5: checkPassword devuelve true con contraseña correcta contra la BD real
    @Test
    @DisplayName("When the correct password is checked against the real DB, it should return true")
    public void testCheckPasswordIntegration() {
        UserDTO userDTO = new UserDTO(null, "check@test.com", "User",
                "es", List.of(), List.of("USER"), "mypassword", null);
        userService.save(userDTO);

        assertTrue(userService.checkPassword("check@test.com", "mypassword"));
    }
}