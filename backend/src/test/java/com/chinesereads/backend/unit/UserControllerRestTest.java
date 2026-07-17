package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.chinesereads.backend.Controller.UserControllerRest;
import com.chinesereads.backend.Service.UserService;
import com.chinesereads.backend.dto.UserDTO;

/**
 * Verifies that GDPR consent is enforced server-side at signup: a request that does
 * not accept the terms of use is rejected (400) and no account is ever created, so a
 * stale cached page or a direct API call cannot register without genuine consent.
 */
public class UserControllerRestTest {

    private UserService userService;
    private UserControllerRest controller;

    @BeforeEach
    public void setUp() {
        userService = mock(UserService.class);
        controller = new UserControllerRest(userService);
        // The success path builds a Location header via ServletUriComponentsBuilder,
        // which needs a current request in the context.
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private UserDTO dto(boolean termsAccepted) {
        return new UserDTO(null, "new@test.com", "New User", "en",
                List.of(), List.of("USER"), "password123", null, termsAccepted);
    }

    @Test
    @DisplayName("Signup without accepting the terms is rejected (400) and never creates a user")
    public void testSignupRejectedWithoutConsent() {
        ResponseEntity<?> response = controller.registerUser(dto(false));

        assertEquals(400, response.getStatusCode().value());
        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("Signup that accepts the terms proceeds to create the account")
    public void testSignupProceedsWithConsent() {
        when(userService.save(any())).thenReturn(dto(true));

        ResponseEntity<?> response = controller.registerUser(dto(true));

        assertEquals(201, response.getStatusCode().value());
        verify(userService).save(any());
    }

    private UserDTO dtoWithReferral(String referral) {
        return new UserDTO(null, "new@test.com", "New User", "en",
                List.of(), List.of("USER"), "password123", null, true, null, null, referral);
    }

    @Test
    @DisplayName("The signup ?ref code is sanitized (charset, case, length) before saving")
    public void testSignupSanitizesReferral() {
        when(userService.save(any())).thenReturn(dto(true));
        ArgumentCaptor<UserDTO> saved = ArgumentCaptor.forClass(UserDTO.class);

        controller.registerUser(dtoWithReferral("  maría-20! "));

        verify(userService).save(saved.capture());
        // Accents and '!' are stripped, the rest upper-cased: only [A-Z0-9_-] survives.
        assertEquals("MARA-20", saved.getValue().referralSource());
    }

    @Test
    @DisplayName("A junk-only or missing ?ref code is stored as null, never as an empty string")
    public void testSignupDropsJunkReferral() {
        when(userService.save(any())).thenReturn(dto(true));
        ArgumentCaptor<UserDTO> saved = ArgumentCaptor.forClass(UserDTO.class);

        controller.registerUser(dtoWithReferral("¡¡¡···!!!"));
        controller.registerUser(dtoWithReferral(null));

        verify(userService, org.mockito.Mockito.times(2)).save(saved.capture());
        assertEquals(null, saved.getAllValues().get(0).referralSource());
        assertEquals(null, saved.getAllValues().get(1).referralSource());
    }
}
