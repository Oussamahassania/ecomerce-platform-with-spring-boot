package com.ecomerce.ecomerce_web.controllers;

import com.ecomerce.ecomerce_web.config.SecurityConfig;
import com.ecomerce.ecomerce_web.config.TestSecurityConfig;
import com.ecomerce.ecomerce_web.controller.PaymentController;
import com.ecomerce.ecomerce_web.dtos.PaymentRequestDto;
import com.ecomerce.ecomerce_web.dtos.PaymentResponseDto;
import com.ecomerce.ecomerce_web.entity.PaymentStatus;
import com.ecomerce.ecomerce_web.security.JwtUtils;
import com.ecomerce.ecomerce_web.security.UserDetailsServiceImpl;
import com.ecomerce.ecomerce_web.services.PaymentService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private PaymentService paymentService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    private PaymentResponseDto paymentResponse;
    private PaymentRequestDto paymentRequest;

    @BeforeEach
    void setUp() {
        paymentResponse = new PaymentResponseDto();
        paymentResponse.setId(1L);
        paymentResponse.setOrderId(10L);
        paymentResponse.setStatus(PaymentStatus.PAID);
        paymentResponse.setAmount(BigDecimal.valueOf(500));
        paymentResponse.setPaymentReference("PAY-ABC123");

        paymentRequest = new PaymentRequestDto();
        paymentRequest.setPaymentMethod("CARD");
    }

    @Nested
    @DisplayName("POST /api/payment/pay/{orderId}")
    class Pay {

        @Test
        @DisplayName("user should process payment and get 200")
        void userShouldPay() throws Exception {
            when(paymentService.processPayment(
                    eq(10L), any(PaymentRequestDto.class),
                    isNull(), any(UserDetails.class)))
                    .thenReturn(paymentResponse);

            mockMvc.perform(post("/api/payment/pay/10")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(paymentRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PAID"))
                    .andExpect(jsonPath("$.paymentReference").value("PAY-ABC123"));
        }

        @Test
        @DisplayName("should pass idempotency key from header to service")
        void shouldPassIdempotencyKey() throws Exception {
            when(paymentService.processPayment(
                    eq(10L), any(PaymentRequestDto.class),
                    eq("key-123"), any(UserDetails.class)))
                    .thenReturn(paymentResponse);

            mockMvc.perform(post("/api/payment/pay/10")
                            .with(user("user@test.com").roles("USER"))
                            .header("idempotency-key", "key-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(paymentRequest)))
                    .andExpect(status().isOk());

            verify(paymentService).processPayment(
                    eq(10L), any(), eq("key-123"), any());
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(post("/api/payment/pay/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(paymentRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/payment/{orderId}")
    class GetStatus {

        @Test
        @DisplayName("user should get payment status")
        void userShouldGetStatus() throws Exception {
            when(paymentService.getPaymentStatus(eq(10L), any(UserDetails.class)))
                    .thenReturn(paymentResponse);

            mockMvc.perform(get("/api/payment/10")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PAID"));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(get("/api/payment/10"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/payment/refund/{orderId}")
    class Refund {

        @Test
        @DisplayName("user should get refund")
        void userShouldRefund() throws Exception {
            paymentResponse.setStatus(PaymentStatus.REFUNDED);
            when(paymentService.refund(eq(10L), any(UserDetails.class)))
                    .thenReturn(paymentResponse);

            mockMvc.perform(post("/api/payment/refund/10")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REFUNDED"));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(post("/api/payment/refund/10"))
                    .andExpect(status().isForbidden());
        }
    }
}