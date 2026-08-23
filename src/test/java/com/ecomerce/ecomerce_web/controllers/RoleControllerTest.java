package com.ecomerce.ecomerce_web.controllers;

import com.ecomerce.ecomerce_web.config.SecurityConfig;
import com.ecomerce.ecomerce_web.config.TestSecurityConfig;
import com.ecomerce.ecomerce_web.controller.RoleController;
import com.ecomerce.ecomerce_web.security.JwtUtils;
import com.ecomerce.ecomerce_web.security.UserDetailsServiceImpl;
import com.ecomerce.ecomerce_web.services.RoleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class RoleControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RoleService roleService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    @Nested
    @DisplayName("PUT /api/roles/change")
    class ChangeRole {

        @Test
        @DisplayName("admin should change user role")
        void adminShouldChangeRole() throws Exception {
            doNothing().when(roleService).changeRole(1L, "admin");

            mockMvc.perform(put("/api/roles/change")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .param("userId", "1")
                            .param("roleName", "admin"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Role updated successfully"));

            verify(roleService).changeRole(1L, "admin");
        }

        @Test
        @DisplayName("regular user should get 403")
        void userShouldBeDenied() throws Exception {
            mockMvc.perform(put("/api/roles/change")
                            .with(user("user@test.com").roles("USER"))
                            .param("userId", "1")
                            .param("roleName", "admin"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(roleService);
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(put("/api/roles/change")
                            .param("userId", "1")
                            .param("roleName", "admin"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(roleService);
        }
    }
}