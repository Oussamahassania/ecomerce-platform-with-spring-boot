package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.*;
import com.ecomerce.ecomerce_web.entity.*;
import com.ecomerce.ecomerce_web.exception.InvalidRequestException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.exception.UnauthorizedActionException;
import com.ecomerce.ecomerce_web.mapper.OrderMapper;
import com.ecomerce.ecomerce_web.repository.OrderRepository;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private EmailService emailService;
    @Mock private UserDetails userDetails;

    private OrderService orderService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, productRepository,
                userRepository, orderMapper, emailService);

        user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setFullName("Test User");

        product = new Product();
        product.setId(100L);
        product.setName("iPhone 15 Pro");
        product.setPrice(BigDecimal.valueOf(500));
        product.setStock(10);
    }

    private void asRegularUser() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .when(userDetails).getAuthorities();
        lenient().when(userDetails.getUsername()).thenReturn("user@test.com");
    }

    private void asAdmin() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(userDetails).getAuthorities();
    }


    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("should create order, deduct stock, and send confirmation email")
        void shouldCreateOrderAndDeductStock() {
            asRegularUser();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(productRepository.findById(100L)).thenReturn(Optional.of(product));

            OrderItemRequestDto itemDto = new OrderItemRequestDto();
            itemDto.setProductId(100L);
            itemDto.setQuantity(3);
            OrderRequestDto dto = new OrderRequestDto();
            dto.setItems(List.of(itemDto));

            Order savedOrder = new Order();
            savedOrder.setId(55L);
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

            OrderResponseDto responseDto = new OrderResponseDto();
            responseDto.setId(55L);
            responseDto.setTotalAmount(BigDecimal.valueOf(1500));
            when(orderMapper.toDto(savedOrder)).thenReturn(responseDto);

            OrderResponseDto result = orderService.createOrder(dto, userDetails);

            assertThat(result.getId()).isEqualTo(55L);
            assertThat(product.getStock()).isEqualTo(7); // 10 - 3
            verify(productRepository).save(product);
            verify(emailService).sendOrderConfirmation(
                    eq("user@test.com"), eq("Test User"), eq(55L), any(), any());
        }

        @Test
        @DisplayName("should throw when stock is insufficient")
        void shouldRejectInsufficientStock() {
            asRegularUser();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            product.setStock(1);
            when(productRepository.findById(100L)).thenReturn(Optional.of(product));

            OrderItemRequestDto itemDto = new OrderItemRequestDto();
            itemDto.setProductId(100L);
            itemDto.setQuantity(5);
            OrderRequestDto dto = new OrderRequestDto();
            dto.setItems(List.of(itemDto));

            assertThatThrownBy(() -> orderService.createOrder(dto, userDetails))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Stock Is Not Enough");

            verify(orderRepository, never()).save(any());
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("admin placing order for another user should use the target userId")
        void adminShouldOrderOnBehalfOfUser() {
            asAdmin();
            User targetUser = new User();
            targetUser.setId(2L);
            targetUser.setEmail("target@test.com");
            targetUser.setFullName("Target User");

            when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
            when(productRepository.findById(100L)).thenReturn(Optional.of(product));

            OrderItemRequestDto itemDto = new OrderItemRequestDto();
            itemDto.setProductId(100L);
            itemDto.setQuantity(1);
            OrderRequestDto dto = new OrderRequestDto();
            dto.setItems(List.of(itemDto));
            dto.setUserId(2L);

            Order savedOrder = new Order();
            savedOrder.setId(60L);
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            OrderResponseDto responseDto = new OrderResponseDto();
            responseDto.setId(60L);
            when(orderMapper.toDto(savedOrder)).thenReturn(responseDto);

            orderService.createOrder(dto, userDetails);

            verify(userRepository).findById(2L);
            verify(userRepository, never()).findByEmail(any());
            verify(emailService).sendOrderConfirmation(
                    eq("target@test.com"), eq("Target User"), any(), any(), any());
        }

        @Test
        @DisplayName("should throw when a product in the order does not exist")
        void shouldThrowWhenProductMissing() {
            asRegularUser();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            OrderItemRequestDto itemDto = new OrderItemRequestDto();
            itemDto.setProductId(999L);
            itemDto.setQuantity(1);
            OrderRequestDto dto = new OrderRequestDto();
            dto.setItems(List.of(itemDto));

            assertThatThrownBy(() -> orderService.createOrder(dto, userDetails))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }


    @Nested
    @DisplayName("getOrderById")
    class GetOrderById {

        @Test
        @DisplayName("owner should be able to view their own order")
        void ownerCanViewOwnOrder() {
            asRegularUser();
            Order order = new Order();
            order.setId(1L);
            order.setUser(user);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            OrderResponseDto dto = new OrderResponseDto();
            when(orderMapper.toDto(order)).thenReturn(dto);

            OrderResponseDto result = orderService.getOrderById(1L, userDetails);

            assertThat(result).isEqualTo(dto);
        }

        @Test
        @DisplayName("non-owner non-admin should be denied access")
        void nonOwnerShouldBeDenied() {
            asRegularUser();
            User otherUser = new User();
            otherUser.setEmail("someoneelse@test.com");
            Order order = new Order();
            order.setId(1L);
            order.setUser(otherUser);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.getOrderById(1L, userDetails))
                    .isInstanceOf(UnauthorizedActionException.class);
        }

        @Test
        @DisplayName("admin should be able to view any order")
        void adminCanViewAnyOrder() {
            asAdmin();
            User otherUser = new User();
            otherUser.setEmail("someoneelse@test.com");
            Order order = new Order();
            order.setId(1L);
            order.setUser(otherUser);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            OrderResponseDto dto = new OrderResponseDto();
            when(orderMapper.toDto(order)).thenReturn(dto);

            assertThatCode(() -> orderService.getOrderById(1L, userDetails))
                    .doesNotThrowAnyException();
        }
    }


    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("should restock items and mark order cancelled")
        void shouldCancelAndRestock() {
            asRegularUser();
            Order order = new Order();
            order.setId(1L);
            order.setUser(user);
            order.setStatus(Status.PENDING);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(3);
            order.setOrderItems(List.of(item));

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            OrderResponseDto dto = new OrderResponseDto();
            when(orderMapper.toDto(order)).thenReturn(dto);

            orderService.cancelOrder(1L, userDetails);

            assertThat(product.getStock()).isEqualTo(13); // 10 + 3 restocked
            assertThat(order.getStatus()).isEqualTo(Status.CANCELLED);
            verify(emailService).sendOrderCancellation(eq("user@test.com"), eq("Test User"), eq(1L));
        }

        @Test
        @DisplayName("should throw when order is already cancelled")
        void shouldRejectDoubleCancellation() {
            asRegularUser();
            Order order = new Order();
            order.setId(1L);
            order.setUser(user);
            order.setStatus(Status.CANCELLED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, userDetails))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Already canceled");
        }

        @Test
        @DisplayName("non-owner should be denied cancellation")
        void nonOwnerCannotCancel() {
            asRegularUser();
            User otherUser = new User();
            otherUser.setEmail("someoneelse@test.com");
            Order order = new Order();
            order.setId(1L);
            order.setUser(otherUser);
            order.setStatus(Status.PENDING);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, userDetails))
                    .isInstanceOf(UnauthorizedActionException.class);
        }
    }


    @Test
    @DisplayName("updateOrderStatus should update status and notify user")
    void shouldUpdateOrderStatus() {
        Order order = new Order();
        order.setId(1L);
        order.setUser(user);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        OrderResponseDto dto = new OrderResponseDto();
        when(orderMapper.toDto(order)).thenReturn(dto);

        orderService.updateOrderStatus(1L, Status.SHIPPED);

        assertThat(order.getStatus()).isEqualTo(Status.SHIPPED);
        verify(emailService).sendOrderStatusUpdate("user@test.com", "Test User", 1L, "SHIPPED");
    }


    @Test
    @DisplayName("deleteOrder should throw when order not found")
    void shouldThrowWhenDeletingMissingOrder() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getTotalRevenue should sum totalAmount across all orders")
    void shouldSumTotalRevenue() {
        Order o1 = new Order(); o1.setTotalAmount(BigDecimal.valueOf(100));
        Order o2 = new Order(); o2.setTotalAmount(BigDecimal.valueOf(250));
        when(orderRepository.findAll()).thenReturn(List.of(o1, o2));

        BigDecimal result = orderService.getTotalRevenue();

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(350));
    }
}