package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.OrderItemsResponseDto;
import com.ecomerce.ecomerce_web.entity.Order;
import com.ecomerce.ecomerce_web.entity.OrderItem;
import com.ecomerce.ecomerce_web.entity.Product;
import com.ecomerce.ecomerce_web.entity.User;
import com.ecomerce.ecomerce_web.exception.InvalidRequestException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.exception.UnauthorizedActionException;
import com.ecomerce.ecomerce_web.mapper.OrderItemMapper;
import com.ecomerce.ecomerce_web.repository.OrderItemRepository;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class OrderItemServiceTest {

    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private UserDetails userDetails;
    @Mock private ProductRepository productRepository;

    private OrderItemService orderItemService;

    private User owner;
    private Order order;
    private Product product;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        orderItemService = new OrderItemService(orderItemRepository, orderItemMapper,productRepository);

        owner = new User();
        owner.setEmail("owner@test.com");

        order = new Order();
        order.setUser(owner);

        product = new Product();
        product.setId(1L);
        product.setName("iPhone");
        product.setPrice(BigDecimal.valueOf(500));
        product.setStock(10);

        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
    }

    @Test
    @DisplayName("owner can view their order item")
    void ownerCanView() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();
        when(userDetails.getUsername()).thenReturn("owner@test.com");
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));
        when(orderItemMapper.toDto(orderItem)).thenReturn(new OrderItemsResponseDto());

        assertThatCode(() -> orderItemService.getOrderItemById(1L, userDetails))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("non-owner non-admin is denied")
    void nonOwnerDenied() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();
        when(userDetails.getUsername()).thenReturn("stranger@test.com");
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> orderItemService.getOrderItemById(1L, userDetails))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    @DisplayName("updateQuantity should adjust stock by the difference")
    void shouldAdjustStockByDifference() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();
        when(userDetails.getUsername()).thenReturn("owner@test.com");
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));
        when(orderItemRepository.save(orderItem)).thenReturn(orderItem);
        when(orderItemMapper.toDto(orderItem)).thenReturn(new OrderItemsResponseDto());

        orderItemService.updateQuantity(1L, 5, userDetails); // was 2, now 5 → diff = 3

        assertThat(product.getStock()).isEqualTo(7); // 10 - 3
        assertThat(orderItem.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("updateQuantity should restock when quantity is decreased")
    void shouldRestockWhenQuantityDecreased() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();
        when(userDetails.getUsername()).thenReturn("owner@test.com");
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));
        when(orderItemRepository.save(orderItem)).thenReturn(orderItem);
        when(orderItemMapper.toDto(orderItem)).thenReturn(new OrderItemsResponseDto());

        orderItemService.updateQuantity(1L, 1, userDetails); // was 2, now 1 → diff = -1

        assertThat(product.getStock()).isEqualTo(11); // 10 - (-1) = 11
    }

    @Test
    @DisplayName("updateQuantity should throw when increase exceeds available stock")
    void shouldRejectWhenIncreaseExceedsStock() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();
        when(userDetails.getUsername()).thenReturn("owner@test.com");
        product.setStock(2);
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> orderItemService.updateQuantity(1L, 100, userDetails))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("removeOrderItem should restock and delete")
    void shouldRestockAndDeleteOnRemove() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();
        when(userDetails.getUsername()).thenReturn("owner@test.com");
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));

        orderItemService.removeOrderItem(1L, userDetails);

        assertThat(product.getStock()).isEqualTo(12); // 10 + 2
        verify(orderItemRepository).delete(orderItem);
    }

    @Test
    @DisplayName("getItemsByOrderId should throw when no items found")
    void shouldThrowWhenNoItemsForOrder() {
        when(orderItemRepository.findByOrderId(999L)).thenReturn(List.of());

        assertThatThrownBy(() -> orderItemService.getItemsByOrderId(999L, userDetails))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}