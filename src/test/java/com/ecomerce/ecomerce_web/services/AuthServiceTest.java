package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.AuthResponse;
import com.ecomerce.ecomerce_web.dtos.LoginRequest;
import com.ecomerce.ecomerce_web.dtos.RegisterRequest;
import com.ecomerce.ecomerce_web.entity.Role;
import com.ecomerce.ecomerce_web.entity.User;
import com.ecomerce.ecomerce_web.exception.DuplicateResourceException;
import com.ecomerce.ecomerce_web.exception.InvalidRequestException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.repository.RoleRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import com.ecomerce.ecomerce_web.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtils jwtUtils;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RoleRepository roleRepository;
    @Mock private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtUtils, authenticationManager,
                passwordEncoder, roleRepository, emailService);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register new user, hash password, and send verification email")
        void shouldRegisterNewUser() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@test.com");
            request.setFullName("New User");
            request.setPassword("plainPassword123");

            when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
            Role userRole = new Role();
            userRole.setName("USER");
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode("plainPassword123")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            String result = authService.register(request);

            assertThat(result).contains("Registration successful");

            var captor = org.mockito.ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getPassword()).isEqualTo("hashedPassword"); // never stores plaintext
            assertThat(saved.isEmailVerified()).isFalse();
            assertThat(saved.getVerificationToken()).isNotBlank();

            verify(emailService).sendVerificationEmail(eq("new@test.com"), eq("New User"), anyString());
        }

        @Test
        @DisplayName("should reject registration with already-used email")
        void shouldRejectDuplicateEmail() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("existing@test.com");

            when(userRepository.findByEmail("existing@test.com"))
                    .thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(userRepository, never()).save(any());
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("should throw when USER role is missing from DB")
        void shouldThrowWhenRoleMissing() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@test.com");

            when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
            when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should return JWT token for verified user with correct credentials")
        void shouldLoginSuccessfully() {
            LoginRequest request = new LoginRequest();
            request.setEmail("user@test.com");
            request.setPassword("correctPassword");

            User user = new User();
            user.setEmail("user@test.com");
            user.setEmailVerified(true);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(jwtUtils.generateToken(user)).thenReturn("fake-jwt-token");

            AuthResponse result = authService.login(request);

            assertThat(result.getAccessToken()).isEqualTo("fake-jwt-token");
            verify(authenticationManager).authenticate(any());
        }

        @Test
        @DisplayName("should reject login for unverified email")
        void shouldRejectUnverifiedEmail() {
            LoginRequest request = new LoginRequest();
            request.setEmail("user@test.com");

            User user = new User();
            user.setEmailVerified(false);
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("verify your email");

            verifyNoInteractions(authenticationManager);
        }

        @Test
        @DisplayName("should reject login for nonexistent email")
        void shouldRejectNonexistentEmail() {
            LoginRequest request = new LoginRequest();
            request.setEmail("ghost@test.com");
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("should verify email and clear token on valid token")
        void shouldVerifyEmail() {
            User user = new User();
            user.setEmailVerified(false);
            user.setVerificationToken("valid-token");
            when(userRepository.findByVerificationToken("valid-token")).thenReturn(Optional.of(user));

            String result = authService.verifyEmail("valid-token");

            assertThat(result).contains("verified successfully");
            assertThat(user.isEmailVerified()).isTrue();
            assertThat(user.getVerificationToken()).isNull();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should throw on invalid or expired token")
        void shouldRejectInvalidToken() {
            when(userRepository.findByVerificationToken("bad-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail("bad-token"))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }
}