package com.ecomerce.ecomerce_web.controllers;
import com.ecomerce.ecomerce_web.config.SecurityConfig;
import com.ecomerce.ecomerce_web.config.TestSecurityConfig;
import com.ecomerce.ecomerce_web.controller.FileController;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.security.JwtUtils;
import com.ecomerce.ecomerce_web.security.UserDetailsServiceImpl;
import com.ecomerce.ecomerce_web.services.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class FileControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private FileStorageService fileStorageService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    @Nested
    @DisplayName("POST /api/files/uplaod")
    class Upload {

        @Test
        @DisplayName("admin should upload file and get URL")
        void adminShouldUploadFile() throws Exception {
            when(fileStorageService.saveFile(any()))
                    .thenReturn("/api/files/550e8400-e29b-41d4-a716-446655440000.png");

            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.png", "image/png", "fake-content".getBytes());

            mockMvc.perform(multipart("/api/files/uplaod")
                            .file(file)
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(content().string("/api/files/550e8400-e29b-41d4-a716-446655440000.png"));
        }

        @Test
        @DisplayName("regular user should get 403")
        void userShouldBeDenied() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.png", "image/png", "content".getBytes());

            mockMvc.perform(multipart("/api/files/uplaod")
                            .file(file)
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(fileStorageService);
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.png", "image/png", "content".getBytes());

            mockMvc.perform(multipart("/api/files/uplaod")
                            .file(file))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/files/{filename}")
    class GetFile {

        @Test
        @DisplayName("should return file for any user (public)")
        void shouldReturnFile() throws Exception {
            Resource resource = new ByteArrayResource("fake-image-bytes".getBytes()) {
                @Override
                public String getFilename() {
                    return "550e8400-e29b-41d4-a716-446655440000.png";
                }
            };

            when(fileStorageService.loadFile("550e8400-e29b-41d4-a716-446655440000.png"))
                    .thenReturn(resource);

            mockMvc.perform(get("/api/files/550e8400-e29b-41d4-a716-446655440000.png"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Content-Disposition"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }

        @Test
        @DisplayName("should return 404 when file not found")
        void shouldReturn404WhenFileMissing() throws Exception {
            when(fileStorageService.loadFile(any()))
                    .thenThrow(new ResourceNotFoundException("File not found"));

            mockMvc.perform(get("/api/files/550e8400-e29b-41d4-a716-446655440000.png"))
                    .andExpect(status().isNotFound());
        }
    }
}
