package com.ecomerce.ecomerce_web.integrationtesting;

import com.ecomerce.ecomerce_web.config.TestCacheConfig;
import com.ecomerce.ecomerce_web.config.TestMailConfig;
import com.ecomerce.ecomerce_web.dtos.LoginRequest;
import com.ecomerce.ecomerce_web.dtos.UserRequestDto;
import com.ecomerce.ecomerce_web.entity.Role;
import com.ecomerce.ecomerce_web.entity.User;
import com.ecomerce.ecomerce_web.repository.RoleRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestMailConfig.class, TestCacheConfig.class})
class UserIntegrationTest {

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

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role userRole = new Role();
        userRole.setName("USER");
        roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        roleRepository.save(adminRole);

        registerUserDirectly("admin@example.com", "Passw0rd!", "ADMIN");
        registerUserDirectly("user@example.com", "Passw0rd!", "USER");

        adminToken = loginAndGetToken("admin@example.com", "Passw0rd!");
        userToken = loginAndGetToken("user@example.com", "Passw0rd!");
    }

    // ---------- Create ----------

    @Test
    void createUser_asAdmin_returns200AndPersists() throws Exception {
        UserRequestDto dto = sampleUserRequest("New Guy", "newguy@example.com");

        mockMvc.perform(post("/api/users/create")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newguy@example.com"));

        assertThat(userRepository.findByEmail("newguy@example.com")).isPresent();
    }

    @Test
    void createUser_asRegularUser_returns403() throws Exception {
        UserRequestDto dto = sampleUserRequest("New Guy", "newguy2@example.com");

        mockMvc.perform(post("/api/users/create")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_noToken_returns403() throws Exception {
        UserRequestDto dto = sampleUserRequest("New Guy", "newguy3@example.com");

        mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_blankEmail_returns400() throws Exception {
        UserRequestDto dto = sampleUserRequest("New Guy", "");

        mockMvc.perform(post("/api/users/create")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ---------- Update ----------

    @Test
    void updateUser_asAdmin_returns200AndUpdatesFields() throws Exception {
        Long targetId = registerUserDirectly("target@example.com", "Passw0rd!", "USER").getId();

        UserRequestDto dto = sampleUserRequest("Updated Name", "target@example.com");

        mockMvc.perform(put("/api/users/update/" + targetId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));
    }

    @Test
    void updateUser_nonExistentId_returns404() throws Exception {
        UserRequestDto dto = sampleUserRequest("Nobody", "nobody@example.com");

        mockMvc.perform(put("/api/users/update/99999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_asRegularUser_returns403() throws Exception {
        Long targetId = registerUserDirectly("target2@example.com", "Passw0rd!", "USER").getId();
        UserRequestDto dto = sampleUserRequest("Updated Name", "target2@example.com");

        mockMvc.perform(put("/api/users/update/" + targetId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ---------- Get by id ----------

    @Test
    void getById_asAdmin_returns200() throws Exception {
        Long targetId = registerUserDirectly("target3@example.com", "Passw0rd!", "USER").getId();

        mockMvc.perform(get("/api/users/" + targetId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("target3@example.com"));
    }

    @Test
    void getById_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/users/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_asRegularUser_returns403() throws Exception {
        Long targetId = registerUserDirectly("target4@example.com", "Passw0rd!", "USER").getId();

        mockMvc.perform(get("/api/users/" + targetId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isForbidden());
    }

    // ---------- Get all ----------

    @Test
    void getAllUsers_asAdmin_returns200WithList() throws Exception {
        mockMvc.perform(get("/api/users/all")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)); // admin + user seeded in setUp
    }

    @Test
    void getAllUsers_asRegularUser_returns403() throws Exception {
        mockMvc.perform(get("/api/users/all")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ---------- Delete ----------

    @Test
    void deleteUser_asAdmin_returns200() throws Exception {
        Long targetId = registerUserDirectly("target5@example.com", "Passw0rd!", "USER").getId();

        mockMvc.perform(delete("/api/users/delete/" + targetId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(targetId)).isEmpty();
    }

    @Test
    void deleteUser_asRegularUser_returns403() throws Exception {
        Long targetId = registerUserDirectly("target6@example.com", "Passw0rd!", "USER").getId();

        mockMvc.perform(delete("/api/users/delete/" + targetId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_noToken_returns403() throws Exception {
        mockMvc.perform(delete("/api/users/delete/1"))
                .andExpect(status().isForbidden());
    }

    // ---------- Helpers ----------

    private UserRequestDto sampleUserRequest(String fullName, String email) {
        UserRequestDto dto = new UserRequestDto();
        dto.setFullName(fullName);
        dto.setEmail(email);
        dto.setPassword("Passw0rd!");
        dto.setDateOfBirth(LocalDate.of(1995, 1, 1));
        return dto;
    }

    private User registerUserDirectly(String email, String rawPassword, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = User.builder()
                .fullName("Test User")
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .emailVerified(true)
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