package com.ecomerce.ecomerce_web.controllers;

import com.ecomerce.ecomerce_web.config.SecurityConfig;
import com.ecomerce.ecomerce_web.config.TestSecurityConfig;
import com.ecomerce.ecomerce_web.controller.OrderItemController;
import com.ecomerce.ecomerce_web.dtos.OrderItemsResponseDto;
import com.ecomerce.ecomerce_web.security.JwtUtils;
import com.ecomerce.ecomerce_web.security.UserDetailsServiceImpl;
import com.ecomerce.ecomerce_web.services.OrderItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderItemController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class OrderItemControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private OrderItemService orderItemService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    private OrderItemsResponseDto itemResponse;

    @BeforeEach
    void setUp() {
        itemResponse = new OrderItemsResponseDto(2, BigDecimal.valueOf(500), 1L);
    }

    @Nested
    @DisplayName("GET /api/orderItem/itemByOrderId/{orderId}")
    class GetByOrderId {

        @Test
        @DisplayName("user should get items for their order")
        void shouldGetItems() throws Exception {
            when(orderItemService.getItemsByOrderId(eq(1L), any(UserDetails.class)))
                    .thenReturn(List.of(itemResponse));

            mockMvc.perform(get("/api/orderItem/itemByOrderId/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].quantity").value(2));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(get("/api/orderItem/itemByOrderId/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/orderItem/orderItemById/{orderItemId}")
    class GetById {

        @Test
        @DisplayName("user should get their order item")
        void shouldGetItem() throws Exception {
            when(orderItemService.getOrderItemById(eq(1L), any(UserDetails.class)))
                    .thenReturn(itemResponse);

            mockMvc.perform(get("/api/orderItem/orderItemById/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantity").value(2));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(get("/api/orderItem/orderItemById/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/orderItem/updateQuantity/{id}")
    class UpdateQuantity {

        @Test
        @DisplayName("user should update quantity")
        void shouldUpdateQuantity() throws Exception {
            when(orderItemService.updateQuantity(eq(1L), eq(5), any(UserDetails.class)))
                    .thenReturn(itemResponse);

            mockMvc.perform(put("/api/orderItem/updateQuantity/1")
                            .with(user("user@test.com").roles("USER"))
                            .param("quantity", "5"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(put("/api/orderItem/updateQuantity/1")
                            .param("quantity", "5"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/orderItem/removeOrderItem/{id}")
    class RemoveItem {

        @Test
        @DisplayName("user should remove order item")
        void shouldRemoveItem() throws Exception {
            doNothing().when(orderItemService).removeOrderItem(eq(1L), any(UserDetails.class));

            mockMvc.perform(delete("/api/orderItem/removeOrderItem/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(content().string("OrderItem Removed Successfully"));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(delete("/api/orderItem/removeOrderItem/1"))
                    .andExpect(status().isForbidden());
        }
    }
}