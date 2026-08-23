package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.PaymentRequestDto;
import com.ecomerce.ecomerce_web.dtos.PaymentResponseDto;
import com.ecomerce.ecomerce_web.entity.*;
import com.ecomerce.ecomerce_web.exception.DuplicateResourceException;
import com.ecomerce.ecomerce_web.exception.InvalidRequestException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.exception.UnauthorizedActionException;
import com.ecomerce.ecomerce_web.mapper.PaymentMapper;
import com.ecomerce.ecomerce_web.repository.OrderRepository;
import com.ecomerce.ecomerce_web.repository.PaymentRepository;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private EmailService emailService;
    @Mock private PaymentMapper paymentMapper;
    @Mock private PaymentGateway paymentGateway;
    @Mock private UserDetails userDetails;

    private PaymentService paymentService;

    private User owner;
    private Order order;
    private PaymentRequestDto dto;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, orderRepository,
                productRepository, emailService, paymentMapper, paymentGateway);

        owner = new User();
        owner.setEmail("owner@test.com");
        owner.setFullName("Order Owner");

        order = new Order();
        order.setId(1L);
        order.setUser(owner);
        order.setStatus(Status.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalAmount(BigDecimal.valueOf(200));

        dto = new PaymentRequestDto();
    }

    private void asOwner() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();
        lenient().when(userDetails.getUsername()).thenReturn("owner@test.com");
    }

    private void asAdmin() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(userDetails).getAuthorities();
    }

    @Nested
    @DisplayName("processPayment")
    class ProcessPayment {

        @Test
        @DisplayName("should return cached payment when idempotencyKey already used")
        void shouldReturnCachedPaymentForIdempotentRequest() {
            Payment existing = new Payment();
            when(paymentRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(existing));
            PaymentResponseDto cachedDto = new PaymentResponseDto();
            when(paymentMapper.toDto(existing)).thenReturn(cachedDto);

            PaymentResponseDto result = paymentService.processPayment(1L, dto, "key-123", userDetails);

            assertThat(result).isEqualTo(cachedDto);
            verifyNoInteractions(orderRepository, paymentGateway);
        }

        @Test
        @DisplayName("should mark order PAID and send confirmation on successful charge")
        void shouldProcessSuccessfulPayment() {
            asOwner();
            when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(paymentGateway.charge(dto)).thenReturn(true);
            when(paymentMapper.toDto(any(Payment.class))).thenReturn(new PaymentResponseDto());

            paymentService.processPayment(1L, dto, "key-1", userDetails);

            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(order.getStatus()).isEqualTo(Status.CONFIRMED);
            verify(emailService).sendPaymentConfirmation(
                    eq("owner@test.com"), eq("Order Owner"), eq(1L), eq(order.getTotalAmount()), any());
        }

        @Test
        @DisplayName("should mark order FAILED and skip email on declined charge")
        void shouldProcessFailedPayment() {
            asOwner();
            when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(paymentGateway.charge(dto)).thenReturn(false);
            when(paymentMapper.toDto(any(Payment.class))).thenReturn(new PaymentResponseDto());

            paymentService.processPayment(1L, dto, "key-2", userDetails);

            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("should reject paying for a cancelled order")
        void shouldRejectCancelledOrder() {
            asOwner();
            order.setStatus(Status.CANCELLED);
            when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.processPayment(1L, dto, "key-3", userDetails))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("canceled order");

            verifyNoInteractions(paymentGateway);
        }

        @Test
        @DisplayName("should reject paying for an already-paid order")
        void shouldRejectAlreadyPaidOrder() {
            asOwner();
            order.setPaymentStatus(PaymentStatus.PAID);
            when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.processPayment(1L, dto, "key-4", userDetails))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should reject paying for a refunded order")
        void shouldRejectRefundedOrder() {
            asOwner();
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.processPayment(1L, dto, "key-5", userDetails))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("refunded order");
        }

        @Test
        @DisplayName("non-owner non-admin should be denied")
        void shouldRejectNonOwner() {
            doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();
            when(userDetails.getUsername()).thenReturn("stranger@test.com");
            when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.processPayment(1L, dto, "key-6", userDetails))
                    .isInstanceOf(UnauthorizedActionException.class);
        }

        @Test
        @DisplayName("admin should be able to pay on behalf of another user's order (after ROLE_ADMIN fix)")
        void adminShouldBypassOwnershipCheck() {
            asAdmin();
            when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(paymentGateway.charge(dto)).thenReturn(true);
            when(paymentMapper.toDto(any(Payment.class))).thenReturn(new PaymentResponseDto());

            assertThatCode(() -> paymentService.processPayment(1L, dto, "key-7", userDetails))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getPaymentStatus")
    class GetPaymentStatus {

        @Test
        @DisplayName("owner can view their payment status")
        void ownerCanView() {
            asOwner();
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            Payment payment = new Payment();
            when(paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(payment));
            when(paymentMapper.toDto(payment)).thenReturn(new PaymentResponseDto());

            assertThatCode(() -> paymentService.getPaymentStatus(1L, userDetails))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should throw when no payment exists for the order")
        void shouldThrowWhenNoPaymentExists() {
            asOwner();
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentStatus(1L, userDetails))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("refund")
    class Refund {

        @Test
        @DisplayName("should restock items, mark REFUNDED, and send email")
        void shouldRefundSuccessfully() {
            asOwner();
            order.setPaymentStatus(PaymentStatus.PAID);

            Product product = new Product();
            product.setStock(5);
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(2);
            order.setOrderItems(List.of(item));

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(paymentMapper.toDto(any(Payment.class))).thenReturn(new PaymentResponseDto());

            paymentService.refund(1L, userDetails);

            assertThat(product.getStock()).isEqualTo(7); // 5 + 2
            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(order.getStatus()).isEqualTo(Status.CANCELLED);
            verify(emailService).sendRefundConfirmation(
                    "owner@test.com", "Order Owner", 1L, order.getTotalAmount());
        }

        @Test
        @DisplayName("should reject refunding a non-PAID order")
        void shouldRejectRefundingUnpaidOrder() {
            asOwner();
            order.setPaymentStatus(PaymentStatus.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentService.refund(1L, userDetails))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Only PAID orders");
        }
    }
}
