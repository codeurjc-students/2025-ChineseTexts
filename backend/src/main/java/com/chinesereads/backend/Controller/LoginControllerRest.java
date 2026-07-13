package com.chinesereads.backend.Controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinesereads.backend.Security.jwt.AuthResponse;
import com.chinesereads.backend.Security.jwt.AuthResponse.Status;
import com.chinesereads.backend.Security.jwt.LoginRequest;
import com.chinesereads.backend.Security.jwt.UserLoginService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@CrossOrigin
@RestController
@RequestMapping("/api/auth")
public class LoginControllerRest {    

    private final UserLoginService userLoginService;

    public LoginControllerRest(UserLoginService userLoginService) {
        this.userLoginService = userLoginService;
    }

    @PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
		return userLoginService.login(response, loginRequest);
	}

    @PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refreshToken(@CookieValue(name = "RefreshToken", required = false) String refreshToken, HttpServletResponse response) {

		return userLoginService.refresh(response, refreshToken);
	}

	@PostMapping("/logout")
	public ResponseEntity<AuthResponse> logOut(HttpServletResponse response) {
		return ResponseEntity.ok(new AuthResponse(Status.SUCCESS, userLoginService.logout(response)));
	}

}
