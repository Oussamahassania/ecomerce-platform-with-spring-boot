package com.ecomerce.ecomerce_web.controllers;

import com.ecomerce.ecomerce_web.config.SecurityConfig;
import com.ecomerce.ecomerce_web.config.TestSecurityConfig;
import com.ecomerce.ecomerce_web.controller.AuthController;
import com.ecomerce.ecomerce_web.dtos.AuthResponse;
import com.ecomerce.ecomerce_web.dtos.LoginRequest;
import com.ecomerce.ecomerce_web.dtos.RegisterRequest;
import com.ecomerce.ecomerce_web.security.JwtUtils;
import com.ecomerce.ecomerce_web.security.UserDetailsServiceImpl;
import com.ecomerce.ecomerce_web.services.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    // ── register ──────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/registration")
    class Register {

        @Test
        @DisplayName("should register valid user and return 200")
        void shouldRegisterValidUser() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setFullName("Test User");
            request.setEmail("test@test.com");
            request.setPassword("Test@1234");

            when(authService.register(any(RegisterRequest.class)))
                    .thenReturn("Registration successful! Please check your email to verify your account.");

            mockMvc.perform(post("/api/auth/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(
                            "Registration successful! Please check your email to verify your account."));

            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("should return 400 when fullName is blank")
        void shouldRejectBlankName() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setFullName("");
            request.setEmail("test@test.com");
            request.setPassword("Test@1234");

            mockMvc.perform(post("/api/auth/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.fullName").exists());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("should return 400 when email format is invalid")
        void shouldRejectInvalidEmail() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setFullName("Test User");
            request.setEmail("not-an-email");
            request.setPassword("Test@1234");

            mockMvc.perform(post("/api/auth/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.email").exists());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("should return 400 when password is too short")
        void shouldRejectShortPassword() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setFullName("Test User");
            request.setEmail("test@test.com");
            request.setPassword("Ab@1");

            mockMvc.perform(post("/api/auth/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.password").exists());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("should return 400 when password lacks required characters")
        void shouldRejectWeakPassword() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setFullName("Test User");
            request.setEmail("test@test.com");
            request.setPassword("password123");

            mockMvc.perform(post("/api/auth/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.password").exists());

            verifyNoInteractions(authService);
        }
    }

    // ── login ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("should return token on valid credentials")
        void shouldReturnTokenOnValidLogin() throws Exception {
            LoginRequest request = new LoginRequest("test@test.com", "Test@1234");
            AuthResponse response = new AuthResponse("jwt-token", null);

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("jwt-token"));
        }
    }

    // ── verifyEmail ───────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/auth/verify")
    class VerifyEmail {

        @Test
        @DisplayName("should verify email with valid token")
        void shouldVerifyEmail() throws Exception {
            when(authService.verifyEmail("valid-token"))
                    .thenReturn("Email verified successfully! You can now login.");

            mockMvc.perform(get("/api/auth/verify")
                            .param("token", "valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Email verified successfully! You can now login."));
        }

        @Test
        @DisplayName("should return 400 when token param is missing")
        void shouldReturn400WhenTokenMissing() throws Exception {
            mockMvc.perform(get("/api/auth/verify"))
                    .andExpect(status().isBadRequest());
        }
    }
}