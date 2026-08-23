package com.ecomerce.ecomerce_web.controllers;

import com.ecomerce.ecomerce_web.config.SecurityConfig;
import com.ecomerce.ecomerce_web.config.TestSecurityConfig;
import com.ecomerce.ecomerce_web.controller.CartController;
import com.ecomerce.ecomerce_web.dtos.CartItemRequestDto;
import com.ecomerce.ecomerce_web.dtos.CartResponseDto;
import com.ecomerce.ecomerce_web.dtos.OrderResponseDto;
import com.ecomerce.ecomerce_web.security.JwtUtils;
import com.ecomerce.ecomerce_web.security.UserDetailsServiceImpl;
import com.ecomerce.ecomerce_web.services.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class CartControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CartService cartService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    private CartResponseDto cartResponse;

    @BeforeEach
    void setUp() {
        cartResponse = new CartResponseDto();
        cartResponse.setId(1L);
        cartResponse.setUserId(10L);
    }

    @Nested
    @DisplayName("POST /api/cart/add")
    class AddToCart {

        @Test
        @DisplayName("authenticated user should add to cart")
        void shouldAddToCart() throws Exception {
            CartItemRequestDto dto = new CartItemRequestDto();
            dto.setProductId(1L);
            dto.setQuantity(2);

            when(cartService.addToCart(any(CartItemRequestDto.class), any(UserDetails.class)))
                    .thenReturn(cartResponse);

            mockMvc.perform(post("/api/cart/add")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            CartItemRequestDto dto = new CartItemRequestDto();
            dto.setProductId(1L);
            dto.setQuantity(2);

            mockMvc.perform(post("/api/cart/add")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(cartService);
        }

        @Test
        @DisplayName("should return 400 when productId is null")
        void shouldRejectNullProductId() throws Exception {
            CartItemRequestDto dto = new CartItemRequestDto();
            dto.setProductId(null);
            dto.setQuantity(2);

            mockMvc.perform(post("/api/cart/add")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.productId").exists());

            verifyNoInteractions(cartService);
        }

        @Test
        @DisplayName("should return 400 when quantity is zero")
        void shouldRejectZeroQuantity() throws Exception {
            CartItemRequestDto dto = new CartItemRequestDto();
            dto.setProductId(1L);
            dto.setQuantity(0);

            mockMvc.perform(post("/api/cart/add")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.quantity").exists());

            verifyNoInteractions(cartService);
        }
    }

    @Nested
    @DisplayName("GET /api/cart")
    class GetCart {

        @Test
        @DisplayName("authenticated user should get their cart")
        void shouldGetCart() throws Exception {
            when(cartService.getCart(any(UserDetails.class))).thenReturn(cartResponse);

            mockMvc.perform(get("/api/cart")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(get("/api/cart"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/cart/update/{cartItemId}")
    class UpdateItem {

        @Test
        @DisplayName("should update item quantity")
        void shouldUpdateItem() throws Exception {
            when(cartService.updateCartItem(any(UserDetails.class), eq(1L), eq(5)))
                    .thenReturn(cartResponse);

            mockMvc.perform(put("/api/cart/update/1")
                            .with(user("user@test.com").roles("USER"))
                            .param("quantity", "5"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(put("/api/cart/update/1")
                            .param("quantity", "5"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/cart/remove/{cartItemId}")
    class RemoveItem {

        @Test
        @DisplayName("should remove item from cart")
        void shouldRemoveItem() throws Exception {
            when(cartService.removeFromCart(eq(1L), any(UserDetails.class)))
                    .thenReturn(cartResponse);

            mockMvc.perform(delete("/api/cart/remove/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(delete("/api/cart/remove/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/cart/clear")
    class ClearCart {

        @Test
        @DisplayName("should clear cart and return message")
        void shouldClearCart() throws Exception {
            doNothing().when(cartService).clearCart(any(UserDetails.class));

            mockMvc.perform(delete("/api/cart/clear")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Cart cleared successfully"));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(delete("/api/cart/clear"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/cart/checkout")
    class Checkout {

        @Test
        @DisplayName("should checkout and return order")
        void shouldCheckout() throws Exception {
            OrderResponseDto orderResponse = new OrderResponseDto();
            orderResponse.setId(99L);

            when(cartService.checkout(any(UserDetails.class))).thenReturn(orderResponse);

            mockMvc.perform(post("/api/cart/checkout")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(99L));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(post("/api/cart/checkout"))
                    .andExpect(status().isForbidden());
        }
    }
}