package com.ecomerce.ecomerce_web.controllers;

import com.ecomerce.ecomerce_web.config.SecurityConfig;
import com.ecomerce.ecomerce_web.config.TestSecurityConfig;
import com.ecomerce.ecomerce_web.controller.OrderController;
import com.ecomerce.ecomerce_web.dtos.OrderItemRequestDto;
import com.ecomerce.ecomerce_web.dtos.OrderRequestDto;
import com.ecomerce.ecomerce_web.dtos.OrderResponseDto;
import com.ecomerce.ecomerce_web.entity.Status;
import com.ecomerce.ecomerce_web.security.JwtUtils;
import com.ecomerce.ecomerce_web.security.UserDetailsServiceImpl;
import com.ecomerce.ecomerce_web.services.OrderService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OrderService orderService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    private OrderResponseDto orderResponse;
    private OrderRequestDto orderRequest;

    @BeforeEach
    void setUp() {
        orderResponse = new OrderResponseDto();
        orderResponse.setId(1L);
        orderResponse.setStatus(Status.PENDING);
        orderResponse.setTotalAmount(BigDecimal.valueOf(500));

        OrderItemRequestDto item = new OrderItemRequestDto(1L, 2);
        orderRequest = new OrderRequestDto();
        orderRequest.setItems(List.of(item));
    }

    @Nested
    @DisplayName("POST /api/orders/createOrder")
    class CreateOrder {

        @Test
        @DisplayName("user should create order")
        void userShouldCreateOrder() throws Exception {
            when(orderService.createOrder(any(OrderRequestDto.class), any(UserDetails.class)))
                    .thenReturn(orderResponse);

            mockMvc.perform(post("/api/orders/createOrder")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(orderRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(post("/api/orders/createOrder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(orderRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/orders/AllOrders")
    class GetAllOrders {

        @Test
        @DisplayName("admin should get all orders")
        void adminShouldGetAllOrders() throws Exception {
            when(orderService.getAllOrders()).thenReturn(List.of(orderResponse));

            mockMvc.perform(get("/api/orders/AllOrders")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L));
        }

        @Test
        @DisplayName("regular user should get 403")
        void userShouldBeDenied() throws Exception {
            mockMvc.perform(get("/api/orders/AllOrders")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/orders/order/{orderId}")
    class GetOrderById {

        @Test
        @DisplayName("user should get their own order")
        void userShouldGetOwnOrder() throws Exception {
            when(orderService.getOrderById(eq(1L), any(UserDetails.class)))
                    .thenReturn(orderResponse);

            mockMvc.perform(get("/api/orders/order/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(get("/api/orders/order/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/orders/{id}/status")
    class UpdateStatus {

        @Test
        @DisplayName("admin should update order status")
        void adminShouldUpdateStatus() throws Exception {
            orderResponse.setStatus(Status.SHIPPED);
            when(orderService.updateOrderStatus(1L, Status.SHIPPED)).thenReturn(orderResponse);

            mockMvc.perform(put("/api/orders/1/status")
                            .with(user("admin@test.com").roles("ADMIN"))
                            .param("status", "SHIPPED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SHIPPED"));
        }

        @Test
        @DisplayName("regular user should get 403")
        void userShouldBeDenied() throws Exception {
            mockMvc.perform(put("/api/orders/1/status")
                            .with(user("user@test.com").roles("USER"))
                            .param("status", "SHIPPED"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/orders/{orderId}/cancel")
    class CancelOrder {

        @Test
        @DisplayName("user should cancel their order")
        void userShouldCancelOrder() throws Exception {
            orderResponse.setStatus(Status.CANCELLED);
            when(orderService.cancelOrder(eq(1L), any(UserDetails.class)))
                    .thenReturn(orderResponse);

            mockMvc.perform(put("/api/orders/1/cancel")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(put("/api/orders/1/cancel"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/orders/delete/{id}")
    class DeleteOrder {

        @Test
        @DisplayName("admin should delete order")
        void adminShouldDelete() throws Exception {
            doNothing().when(orderService).deleteOrder(1L);

            mockMvc.perform(delete("/api/orders/delete/1")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(content().string("order deleted successfully"));
        }

        @Test
        @DisplayName("user should get 403 on delete")
        void userShouldBeDenied() throws Exception {
            mockMvc.perform(delete("/api/orders/delete/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/orders/totalRevenue")
    class TotalRevenue {

        @Test
        @DisplayName("admin should get total revenue")
        void adminShouldGetRevenue() throws Exception {
            when(orderService.getTotalRevenue()).thenReturn(BigDecimal.valueOf(9999));

            mockMvc.perform(get("/api/orders/totalRevenue")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(9999));
        }

        @Test
        @DisplayName("user should get 403")
        void userShouldBeDenied() throws Exception {
            mockMvc.perform(get("/api/orders/totalRevenue")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isForbidden());
        }
    }
}