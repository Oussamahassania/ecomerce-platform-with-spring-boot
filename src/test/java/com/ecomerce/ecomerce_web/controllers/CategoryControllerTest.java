package com.ecomerce.ecomerce_web.controllers;

import com.ecomerce.ecomerce_web.config.SecurityConfig;
import com.ecomerce.ecomerce_web.config.TestSecurityConfig;
import com.ecomerce.ecomerce_web.controller.CategoryController;
import com.ecomerce.ecomerce_web.dtos.CategoryRequest;
import com.ecomerce.ecomerce_web.dtos.CategoryResponse;
import com.ecomerce.ecomerce_web.dtos.ProductResponseDto;
import com.ecomerce.ecomerce_web.security.JwtUtils;
import com.ecomerce.ecomerce_web.security.UserDetailsServiceImpl;
import com.ecomerce.ecomerce_web.services.CategoryService;
import com.ecomerce.ecomerce_web.services.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class CategoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CategoryService categoryService;
    @MockitoBean private ProductService productService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    private CategoryResponse categoryResponse;
    private CategoryRequest categoryRequest;

    @BeforeEach
    void setUp() {
        categoryResponse = new CategoryResponse();
        categoryResponse.setId(1L);
        categoryResponse.setName("Electronics");

        categoryRequest = new CategoryRequest();
        categoryRequest.setName("Electronics");
    }

    @Nested
    @DisplayName("POST /api/categories/create")
    class CreateCategory {

        @Test
        @DisplayName("admin should create category and get 201")
        void adminShouldCreateCategory() throws Exception {
            when(categoryService.createCategory(any(CategoryRequest.class)))
                    .thenReturn(categoryResponse);

            mockMvc.perform(post("/api/categories/create")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Electronics"));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(post("/api/categories/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequest)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("regular user should get 403")
        void userShouldBeDenied() throws Exception {
            mockMvc.perform(post("/api/categories/create")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequest)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldRejectBlankName() throws Exception {
            categoryRequest.setName("");

            mockMvc.perform(post("/api/categories/create")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.name").exists());

            verifyNoInteractions(categoryService);
        }
    }

    @Nested
    @DisplayName("GET /api/categories")
    class GetAll {

        @Test
        @DisplayName("should return all categories for anonymous user")
        void shouldReturnAllCategories() throws Exception {
            when(categoryService.getAll()).thenReturn(List.of(categoryResponse));

            mockMvc.perform(get("/api/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Electronics"));
        }
    }

    @Nested
    @DisplayName("GET /api/categories/{id}/products")
    class GetProductsByCategory {

        @Test
        @DisplayName("should return products for given category")
        void shouldReturnProducts() throws Exception {
            ProductResponseDto product = new ProductResponseDto();
            product.setId(1L);
            product.setName("iPhone");
            product.setPrice(BigDecimal.valueOf(999));

            when(productService.getByCategory(eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(product)));

            mockMvc.perform(get("/api/categories/1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("iPhone"));
        }
    }

    @Nested
    @DisplayName("PUT /api/categories/update/{id}")
    class UpdateCategory {

        @Test
        @DisplayName("admin should update category")
        void adminShouldUpdate() throws Exception {
            when(categoryService.updateCategory(eq(1L), any(CategoryRequest.class)))
                    .thenReturn(categoryResponse);

            mockMvc.perform(put("/api/categories/update/1")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Electronics"));
        }

        @Test
        @DisplayName("regular user should get 403")
        void userShouldBeDenied() throws Exception {
            mockMvc.perform(put("/api/categories/update/1")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(categoryRequest)))
                    .andExpect(status().isForbidden());
        }
    }
}