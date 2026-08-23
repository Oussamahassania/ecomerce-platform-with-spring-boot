package com.ecomerce.ecomerce_web.controllers;

import com.ecomerce.ecomerce_web.config.SecurityConfig;
import com.ecomerce.ecomerce_web.config.TestSecurityConfig;
import com.ecomerce.ecomerce_web.controller.UserController;
import com.ecomerce.ecomerce_web.dtos.UserRequestDto;
import com.ecomerce.ecomerce_web.dtos.UserResponseDto;
import com.ecomerce.ecomerce_web.security.JwtUtils;
import com.ecomerce.ecomerce_web.security.UserDetailsServiceImpl;
import com.ecomerce.ecomerce_web.services.UserService;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    private UserResponseDto userResponse;
    private UserRequestDto userRequest;

    @BeforeEach
    void setUp() {
        userResponse = new UserResponseDto();
        userResponse.setId(1L);
        userResponse.setFullName("Test User");
        userResponse.setEmail("test@test.com");

        userRequest = new UserRequestDto();
        userRequest.setFullName("Test User");
        userRequest.setEmail("test@test.com");
        userRequest.setPassword("password123");
        userRequest.setDateOfBirth(LocalDate.of(1995, 1, 1));
    }

    @Nested
    @DisplayName("POST /api/users/create")
    class CreateUser {

        @Test
        @DisplayName("admin should create user")
        void adminShouldCreateUser() throws Exception {
            when(userService.createUser(any(UserRequestDto.class))).thenReturn(userResponse);

            mockMvc.perform(post("/api/users/create")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.email").value("test@test.com"));
        }

        @Test
        @DisplayName("regular user should get 403")
        void userShouldBeDenied() throws Exception {
            mockMvc.perform(post("/api/users/create")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userRequest)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("should return 400 when fullName is blank")
        void shouldRejectBlankName() throws Exception {
            userRequest.setFullName("");

            mockMvc.perform(post("/api/users/create")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.fullName").exists());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("should return 400 when email is invalid")
        void shouldRejectInvalidEmail() throws Exception {
            userRequest.setEmail("not-an-email");

            mockMvc.perform(post("/api/users/create")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.email").exists());

            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetById {

        @Test
        @DisplayName("admin should get user by id")
        void adminShouldGetUser() throws Exception {
            when(userService.getById(1L)).thenReturn(userResponse);

            mockMvc.perform(get("/api/users/1")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @DisplayName("regular user should get 403")
        void userShouldBeDenied() throws Exception {
            mockMvc.perform(get("/api/users/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/users/all")
    class GetAll {

        @Test
        @DisplayName("admin should get all users")
        void adminShouldGetAll() throws Exception {
            when(userService.getAllUsers()).thenReturn(List.of(userResponse));

            mockMvc.perform(get("/api/users/all")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].email").value("test@test.com"));
        }

        @Test
        @DisplayName("regular user should get 403")
        void userShouldBeDenied() throws Exception {
            mockMvc.perform(get("/api/users/all")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/users/update/{id}")
    class UpdateUser {

        @Test
        @DisplayName("admin should update user")
        void adminShouldUpdate() throws Exception {
            when(userService.updateUser(eq(1L), any(UserRequestDto.class))).thenReturn(userResponse);

            mockMvc.perform(put("/api/users/update/1")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @DisplayName("regular user should get 403")
        void userShouldBeDenied() throws Exception {
            mockMvc.perform(put("/api/users/update/1")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/delete/{id}")
    class DeleteUser {

        @Test
        @DisplayName("admin should delete user")
        void adminShouldDelete() throws Exception {
            doNothing().when(userService).deleteUser(1L);

            mockMvc.perform(delete("/api/users/delete/1")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(content().string("user deleted successfully"));

            verify(userService).deleteUser(1L);
        }

        @Test
        @DisplayName("regular user should get 403")
        void userShouldBeDenied() throws Exception {
            mockMvc.perform(delete("/api/users/delete/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }
    }
}
