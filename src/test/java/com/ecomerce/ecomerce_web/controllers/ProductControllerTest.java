package com.ecomerce.ecomerce_web.controllers;
import com.ecomerce.ecomerce_web.config.SecurityConfig;
import com.ecomerce.ecomerce_web.controller.ProductController;
import com.ecomerce.ecomerce_web.dtos.ProductRequestDto;
import com.ecomerce.ecomerce_web.dtos.ProductResponseDto;
import com.ecomerce.ecomerce_web.dtos.StockUpdateRequestDto;
import com.ecomerce.ecomerce_web.security.JwtAuthFilter;
import com.ecomerce.ecomerce_web.security.JwtUtils;
import com.ecomerce.ecomerce_web.security.UserDetailsServiceImpl;
import com.ecomerce.ecomerce_web.services.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, ProductControllerTest.TestSecurityConfig.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private tools.jackson.databind.ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private ProductResponseDto responseDto;
    private ProductRequestDto validRequestDto;

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        JwtAuthFilter jwtAuthFilter(JwtUtils jwtUtils, UserDetailsServiceImpl uds) {
            return new JwtAuthFilter(jwtUtils, uds) {
                @Override
                protected void doFilterInternal(
                        jakarta.servlet.http.HttpServletRequest request,
                        jakarta.servlet.http.HttpServletResponse response,
                        jakarta.servlet.FilterChain filterChain)
                        throws jakarta.servlet.ServletException, java.io.IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }

        @Bean
        org.springframework.cache.CacheManager cacheManager() {
            return new org.springframework.cache.support.NoOpCacheManager();
        }

        @Bean
        tools.jackson.databind.ObjectMapper objectMapper() {
            return new tools.jackson.databind.ObjectMapper();
        }
    }


    @BeforeEach
    void setUp() {
        responseDto = new ProductResponseDto();
        responseDto.setId(1L);
        responseDto.setName("iPhone 15 Pro");
        responseDto.setPrice(BigDecimal.valueOf(999.99));
        responseDto.setStock(50);

        validRequestDto = new ProductRequestDto();
        validRequestDto.setName("iPhone 15 Pro");
        validRequestDto.setDescription("A great phone with a long enough description here");
        validRequestDto.setPrice(BigDecimal.valueOf(999.99));
        validRequestDto.setStock(50);
    }

    @Nested
    @DisplayName("GET /api/products/AllProducts")
    class GetAllProducts {

        @Test
        @DisplayName("should return 200 and product list for anonymous user")
        void shouldReturnAllProductsWithoutAuth() throws Exception {
            when(productService.getAll()).thenReturn(List.of(responseDto));

            mockMvc.perform(get("/api/products/AllProducts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].name").value("iPhone 15 Pro"))
                    .andExpect(jsonPath("$[0].price").value(999.99));
        }

        @Test
        @DisplayName("should return empty list when no products exist")
        void shouldReturnEmptyList() throws Exception {
            when(productService.getAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/products/AllProducts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/products/{id}")
    class GetById {

        @Test
        @DisplayName("should return 200 for any user")
        void shouldReturnProductById() throws Exception {
            when(productService.getById(1L)).thenReturn(responseDto);

            mockMvc.perform(get("/api/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("iPhone 15 Pro"));
        }
    }


    @Nested
    @DisplayName("POST /api/products/createProduct")
    class CreateProduct {

        @Test
        @DisplayName("admin should create product and get 200")
        void adminShouldCreateProduct() throws Exception {
            when(productService.createProduct(any(ProductRequestDto.class))).thenReturn(responseDto);

            mockMvc.perform(post("/api/products/createProduct")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("iPhone 15 Pro"));

            verify(productService).createProduct(any(ProductRequestDto.class));
        }

        @Test
        @DisplayName("unauthenticated user should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(post("/api/products/createProduct")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDto)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(productService);
        }

        @Test
        @DisplayName("regular user should get 403")
        void userShouldBeDenied() throws Exception {
            mockMvc.perform(post("/api/products/createProduct")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDto)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(productService);
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldRejectBlankName() throws Exception {
            validRequestDto.setName("");

            mockMvc.perform(post("/api/products/createProduct")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.name").exists());

            verifyNoInteractions(productService);
        }

        @Test
        @DisplayName("should return 400 when description is too short")
        void shouldRejectShortDescription() throws Exception {
            validRequestDto.setDescription("too short");

            mockMvc.perform(post("/api/products/createProduct")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.description").exists());

            verifyNoInteractions(productService);
        }

        @Test
        @DisplayName("should return 400 when description is too long")
        void shouldRejectLongDescription() throws Exception {
            validRequestDto.setDescription("a".repeat(201));

            mockMvc.perform(post("/api/products/createProduct")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.description").exists());

            verifyNoInteractions(productService);
        }
    }


    @Nested
    @DisplayName("PUT /api/products/updateProduct/{id}")
    class UpdateProduct {

        @Test
        @DisplayName("admin should update product and get 200")
        void adminShouldUpdateProduct() throws Exception {
            when(productService.updateProduct(any(ProductRequestDto.class), eq(1L)))
                    .thenReturn(responseDto);

            mockMvc.perform(put("/api/products/updateProduct/1")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @DisplayName("regular user should get 403 on update")
        void userShouldBeDeniedUpdate() throws Exception {
            mockMvc.perform(put("/api/products/updateProduct/1")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDto)))
                    .andExpect(status().isForbidden());
        }
    }


    @Nested
    @DisplayName("DELETE /api/products/delete/{id}")
    class DeleteProduct {

        @Test
        @DisplayName("admin should delete product and get 200")
        void adminShouldDeleteProduct() throws Exception {
            doNothing().when(productService).delete(1L);

            mockMvc.perform(delete("/api/products/delete/1")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Product deleted successfully"));

            verify(productService).delete(1L);
        }

        @Test
        @DisplayName("unauthenticated should get 403 on delete")
        void unauthenticatedShouldBeDeniedDelete() throws Exception {
            mockMvc.perform(delete("/api/products/delete/1"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(productService);
        }

        @Test
        @DisplayName("regular user should get 403 on delete")
        void userShouldBeDeniedDelete() throws Exception {
            mockMvc.perform(delete("/api/products/delete/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(productService);
        }
    }


    @Nested
    @DisplayName("GET /api/products/search")
    class SearchProducts {

        @Test
        @DisplayName("should return 200 with results for anonymous user")
        void shouldSearchProductsWithoutAuth() throws Exception {
            Page<ProductResponseDto> page = new PageImpl<>(List.of(responseDto));
            when(productService.searchProducts(
                    eq("iPhone"), isNull(), isNull(), isNull(),
                    isNull(), eq("ASC"), eq(0), eq(10)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/products/search")
                            .param("name", "iPhone"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("iPhone 15 Pro"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 200 with empty page when no results")
        void shouldReturnEmptyPage() throws Exception {
            when(productService.searchProducts(
                    eq("nonexistent"), isNull(), isNull(), isNull(),
                    isNull(), eq("ASC"), eq(0), eq(10)))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/products/search")
                            .param("name", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("should pass sort and order params correctly to service")
        void shouldPassSortAndOrderToService() throws Exception {
            when(productService.searchProducts(
                    isNull(), isNull(), isNull(), isNull(),
                    eq("price"), eq("desc"), eq(0), eq(5)))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/products/search")
                            .param("sort", "price")
                            .param("order", "desc")
                            .param("size", "5"))
                    .andExpect(status().isOk());

            verify(productService).searchProducts(
                    isNull(), isNull(), isNull(), isNull(),
                    eq("price"), eq("desc"), eq(0), eq(5));
        }
    }


    @Nested
    @DisplayName("GET /api/products/filter")
    class FilterProducts {

        @Test
        @DisplayName("should filter by price range")
        void shouldFilterByPriceRange() throws Exception {
            Page<ProductResponseDto> page = new PageImpl<>(List.of(responseDto));
            when(productService.searchProducts(
                    isNull(),
                    eq(new BigDecimal("100")),
                    eq(new BigDecimal("1000")),
                    isNull(), isNull(), eq("ASC"), eq(0), eq(10)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/products/filter")
                            .param("minPrice", "100")
                            .param("maxPrice", "1000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1L));
        }

        @Test
        @DisplayName("should filter by categoryId")
        void shouldFilterByCategoryId() throws Exception {
            when(productService.searchProducts(
                    isNull(), isNull(), isNull(), eq(1L),
                    isNull(), eq("ASC"), eq(0), eq(10)))
                    .thenReturn(new PageImpl<>(List.of(responseDto)));

            mockMvc.perform(get("/api/products/filter")
                            .param("categoryId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }


    @Nested
    @DisplayName("PUT /api/products/{id}/stock")
    class UpdateStock {

        @Test
        @DisplayName("admin should update stock and get 200")
        void adminShouldUpdateStock() throws Exception {
            StockUpdateRequestDto stockDto = new StockUpdateRequestDto();
            stockDto.setStock(25);
            responseDto.setStock(25);
            when(productService.updateStock(1L, 25)).thenReturn(responseDto);

            mockMvc.perform(put("/api/products/1/stock")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(stockDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stock").value(25));
        }

        @Test
        @DisplayName("regular user should get 403 on stock update")
        void userShouldBeDeniedStockUpdate() throws Exception {
            StockUpdateRequestDto stockDto = new StockUpdateRequestDto();
            stockDto.setStock(25);

            mockMvc.perform(put("/api/products/1/stock")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(stockDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when stock is negative")
        void shouldRejectNegativeStock() throws Exception {
            StockUpdateRequestDto stockDto = new StockUpdateRequestDto();
            stockDto.setStock(-1);

            mockMvc.perform(put("/api/products/1/stock")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(stockDto)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(productService);
        }
    }


    @Nested
    @DisplayName("GET /api/products/low-stock")
    class GetLowStock {

        @Test
        @DisplayName("admin should get low stock list")
        void adminShouldGetLowStock() throws Exception {
            responseDto.setStock(3);
            when(productService.getLowStockProducts()).thenReturn(List.of(responseDto));

            mockMvc.perform(get("/api/products/low-stock")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].stock").value(3));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(get("/api/products/low-stock"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(productService);
        }

        @Test
        @DisplayName("regular user should get 403")
        void userShouldBeDenied() throws Exception {
            mockMvc.perform(get("/api/products/low-stock")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(productService);
        }
    }
}
