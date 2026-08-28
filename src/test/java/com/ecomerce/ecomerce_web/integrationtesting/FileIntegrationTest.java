package com.ecomerce.ecomerce_web.integrationtesting;

import com.ecomerce.ecomerce_web.config.TestCacheConfig;
import com.ecomerce.ecomerce_web.config.TestMailConfig;
import com.ecomerce.ecomerce_web.dtos.LoginRequest;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestMailConfig.class, TestCacheConfig.class})
class FileIntegrationTest {

    // 1x1 pixel valid JPEG
    private static final byte[] VALID_JPEG_BYTES = Base64.getDecoder().decode(
            "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4n" +
                    "ICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIy" +
                    "MjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAA" +
                    "AAAAAAAAAAAAAAj/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEA" +
                    "AAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k=");

    // 1x1 pixel valid PNG
    private static final byte[] VALID_PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+A8AAQUBAScY42YAAAAA" +
                    "SUVORK5CYII=");

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

    // ---------- Upload ----------

    @Test
    void uploadFile_asAdmin_validJpeg_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", VALID_JPEG_BYTES);

        mockMvc.perform(multipart("/api/files/uplaod")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("/api/files/")));
    }

    @Test
    void uploadFile_asAdmin_validPng_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", VALID_PNG_BYTES);

        mockMvc.perform(multipart("/api/files/uplaod")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void uploadFile_asRegularUser_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", VALID_JPEG_BYTES);

        mockMvc.perform(multipart("/api/files/uplaod")
                        .file(file)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadFile_noToken_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", VALID_JPEG_BYTES);

        mockMvc.perform(multipart("/api/files/uplaod").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadFile_emptyFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/api/files/uplaod")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadFile_tooLarge_returns400() throws Exception {
        byte[] oversized = new byte[2 * 1024 * 1024 + 1]; // just over 2MB cap
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", oversized);

        mockMvc.perform(multipart("/api/files/uplaod")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadFile_disallowedExtension_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.txt", "text/plain", "not an image".getBytes());

        mockMvc.perform(multipart("/api/files/uplaod")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadFile_doubleExtensionAttack_returns400() throws Exception {
        // filename smuggles a dangerous extension before the allowed one
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.php.jpg", "image/jpeg", VALID_JPEG_BYTES);

        mockMvc.perform(multipart("/api/files/uplaod")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadFile_declaredTypeDoesNotMatchActualContent_returns400() throws Exception {
        // real bytes are PNG, but declared/extension claim JPEG - Tika detection catches this
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", VALID_PNG_BYTES);

        mockMvc.perform(multipart("/api/files/uplaod")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadFile_missingFilePart_returns400() throws Exception {
        mockMvc.perform(multipart("/api/files/uplaod")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    // ---------- Get file ----------

    @Test
    void getFile_afterUpload_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", VALID_JPEG_BYTES);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files/uplaod")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String fileUrl = uploadResult.getResponse().getContentAsString();
        // fileUrl looks like /api/files/<uuid>.jpg - no token needed, endpoint is public
        mockMvc.perform(get(fileUrl))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void getFile_nonExistentFile_returns404() throws Exception {
        mockMvc.perform(get("/api/files/00000000-0000-0000-0000-000000000000.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFile_invalidFilenameFormat_returns404() throws Exception {
        mockMvc.perform(get("/api/files/not-a-valid-name.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFile_pathTraversalAttempt_returns400() throws Exception {
        // Spring Security's StrictHttpFirewall rejects any ".." in the URL before
        // it reaches FileController — this is a defense-in-depth layer in front of
        // FileStorageService's own path-traversal check, not the check itself.
        mockMvc.perform(get("/api/files/../../../etc/passwd"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Helpers ----------

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