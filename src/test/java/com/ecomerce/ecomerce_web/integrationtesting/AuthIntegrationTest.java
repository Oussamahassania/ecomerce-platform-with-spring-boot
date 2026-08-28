package com.ecomerce.ecomerce_web.integrationtesting;

import com.ecomerce.ecomerce_web.config.TestMailConfig;
import com.ecomerce.ecomerce_web.dtos.LoginRequest;
import com.ecomerce.ecomerce_web.dtos.RegisterRequest;
import com.ecomerce.ecomerce_web.entity.Role;
import com.ecomerce.ecomerce_web.entity.User;
import com.ecomerce.ecomerce_web.repository.RoleRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestMailConfig.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Seed roles required by AuthService.register()
        Role userRole = new Role();
        userRole.setName("USER");
        roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        roleRepository.save(adminRole);
    }

    // ---------- Registration ----------

    @Test
    void register_withValidData_returnsSuccessMessage() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPassword("Passw0rd!");
        request.setDateOfBirth(LocalDate.of(1995, 1, 1));

        mockMvc.perform(post("/api/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Registration successful")));

        Optional<User> saved = userRepository.findByEmail("jane@example.com");
        org.assertj.core.api.Assertions.assertThat(saved).isPresent();
        org.assertj.core.api.Assertions.assertThat(saved.get().isEmailVerified()).isFalse();
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        registerUserDirectly("jane@example.com", "Passw0rd!", "USER", false);

        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPassword("Passw0rd!");
        request.setDateOfBirth(LocalDate.of(1995, 1, 1));

        mockMvc.perform(post("/api/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_withWeakPassword_returns400WithFieldErrors() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane2@example.com");
        request.setPassword("weak");
        request.setDateOfBirth(LocalDate.of(1995, 1, 1));

        mockMvc.perform(post("/api/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    // ---------- Email verification ----------

    @Test
    void verifyEmail_withValidToken_marksUserVerified() throws Exception {
        User user = registerUserDirectly("verify@example.com", "Passw0rd!", "USER", false);

        mockMvc.perform(get("/api/auth/verify")
                        .param("token", user.getVerificationToken()))
                .andExpect(status().isOk());

        User updated = userRepository.findByEmail("verify@example.com").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.isEmailVerified()).isTrue();
    }

    @Test
    void verifyEmail_withInvalidToken_returns400() throws Exception {
        mockMvc.perform(get("/api/auth/verify")
                        .param("token", "not-a-real-token"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Login ----------

    @Test
    void login_withValidVerifiedUser_returnsToken() throws Exception {
        registerUserDirectly("login@example.com", "Passw0rd!", "USER", true);

        LoginRequest request = new LoginRequest();
        request.setEmail("login@example.com");
        request.setPassword("Passw0rd!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void login_withUnverifiedEmail_returns401() throws Exception {
        registerUserDirectly("unverified@example.com", "Passw0rd!", "USER", false);

        LoginRequest request = new LoginRequest();
        request.setEmail("unverified@example.com");
        request.setPassword("Passw0rd!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        registerUserDirectly("wrongpass@example.com", "Passw0rd!", "USER", true);

        LoginRequest request = new LoginRequest();
        request.setEmail("wrongpass@example.com");
        request.setPassword("TotallyWrong1!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withNonExistentEmail_returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@example.com");
        request.setPassword("Passw0rd!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Token-protected access ----------

    @Test
    void protectedEndpoint_withNoToken_returns401or200DependingOnRule() throws Exception {
        // /api/products/AllProducts is permitAll for GET per SecurityConfig,
        // so this should succeed even with no token. Included to document
        // that public GETs work without auth.
        mockMvc.perform(get("/api/products/AllProducts"))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpoint_withNoToken_returns403() throws Exception {
        // Spring Security treats an unauthenticated request as "anonymous"
        // by default. Since no custom AuthenticationEntryPoint is configured,
        // a @PreAuthorize denial for anonymous users returns 403, not 401.
        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_withRegularUserToken_returns403() throws Exception {
        registerUserDirectly("regular@example.com", "Passw0rd!", "USER", true);
        String token = loginAndGetToken("regular@example.com", "Passw0rd!");

        mockMvc.perform(get("/api/products/low-stock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_withAdminToken_returns200() throws Exception {
        registerUserDirectly("admin@example.com", "Passw0rd!", "ADMIN", true);
        String token = loginAndGetToken("admin@example.com", "Passw0rd!");

        mockMvc.perform(get("/api/products/low-stock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_withMalformedToken_returns403NotServerError() throws Exception {
        // Before the JwtAuthFilter fix, this threw an uncaught MalformedJwtException
        // (500). After the fix, the filter catches it, leaves the request
        // unauthenticated, and @PreAuthorize denies it normally (403).
        mockMvc.perform(get("/api/products/low-stock")
                        .header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_withMissingBearerPrefix_returns403() throws Exception {
        registerUserDirectly("noprefix@example.com", "Passw0rd!", "ADMIN", true);
        String token = loginAndGetToken("noprefix@example.com", "Passw0rd!");

        mockMvc.perform(get("/api/products/low-stock")
                        .header("Authorization", token)) // no "Bearer " prefix
                .andExpect(status().isForbidden());
    }

    // ---------- Helpers ----------

    private User registerUserDirectly(String email, String rawPassword, String roleName, boolean verified) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = User.builder()
                .fullName("Test User")
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .emailVerified(verified)
                .verificationToken(verified ? null : java.util.UUID.randomUUID().toString())
                .dateOfBirth(LocalDate.of(1995, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}