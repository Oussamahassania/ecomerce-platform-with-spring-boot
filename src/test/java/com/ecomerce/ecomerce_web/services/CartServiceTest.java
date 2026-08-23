package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.*;
import com.ecomerce.ecomerce_web.entity.*;
import com.ecomerce.ecomerce_web.exception.InvalidRequestException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.exception.UnauthorizedActionException;
import com.ecomerce.ecomerce_web.mapper.CartMapper;
import com.ecomerce.ecomerce_web.repository.CartItemRepository;
import com.ecomerce.ecomerce_web.repository.CartRepository;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderService orderService;
    @Mock private CartMapper cartMapper;
    @Mock private UserDetails userDetails;

    private CartService cartService;

    private User user;
    private Cart cart;
    private Product product;
    private CartResponseDto cartResponseDto;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, cartItemRepository,
                productRepository, userRepository, orderService, cartMapper);

        user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setFullName("Test User");

        cart = new Cart();
        cart.setId(10L);
        cart.setUser(user);
        cart.setItems(new ArrayList<>());

        product = new Product();
        product.setId(100L);
        product.setName("iPhone 15 Pro");
        product.setPrice(BigDecimal.valueOf(999.99));
        product.setStock(50);
        product.setActive(true);

        cartResponseDto = new CartResponseDto();
        cartResponseDto.setId(10L);

        lenient().when(userDetails.getUsername()).thenReturn("user@test.com");
        lenient().when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        lenient().when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
    }
    @Nested
    @DisplayName("addToCart")
    class addToCart{
        @Test
        @DisplayName("should create new cart item when product not already in cart")
        void shouldAddNewItem(){
            CartItemRequestDto dto = new CartItemRequestDto();
            dto.setProductId(100L);
            dto.setQuantity(2);
            when(productRepository.findById(100L)).thenReturn(Optional.of(product));
            when(cartItemRepository.findByCartIdAndProductId(10L,100L)).thenReturn(Optional.empty());
            when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
            when(cartMapper.toDto(cart)).thenReturn(cartResponseDto);

            CartResponseDto result =  cartService.addToCart(dto,userDetails);

            assertThat(result).isEqualTo(cartResponseDto);

            ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository).save(captor.capture());
            CartItem saved = captor.getValue();
            assertThat(saved.getQuantity()).isEqualTo(2);
            assertThat(saved.getPriceAtAdding()).isEqualByComparingTo(product.getPrice());
            assertThat(saved.getProduct()).isEqualTo(product);
        }
        @Test
        @DisplayName("should increase quantity when product already in cart")
        void shouldIncreaseQuantityForExistingItem() {
            CartItem existing = new CartItem();
            existing.setId(500L);
            existing.setCart(cart);
            existing.setProduct(product);
            existing.setQuantity(3);
            existing.setPriceAtAdding(product.getPrice());

            CartItemRequestDto dto = new CartItemRequestDto();
            dto.setProductId(100L);
            dto.setQuantity(2);

            when(productRepository.findById(100L)).thenReturn(Optional.of(product));
            when(cartItemRepository.findByCartIdAndProductId(10L, 100L)).thenReturn(Optional.of(existing));
            when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
            when(cartMapper.toDto(cart)).thenReturn(cartResponseDto);

            cartService.addToCart(dto, userDetails);

            assertThat(existing.getQuantity()).isEqualTo(5);
            verify(cartItemRepository).save(existing);
        }
        @Test
        @DisplayName("should throw when adding more than available stock (new item)")
        void shouldRejectInsufficientStockForNewItem(){
            product.setStock(1);
            CartItemRequestDto dto = new CartItemRequestDto();
            dto.setProductId(100L);
            dto.setQuantity(5);
            when(productRepository.findById(100L)).thenReturn(Optional.of(product));
            when(cartItemRepository.findByCartIdAndProductId(10L, 100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addToCart(dto, userDetails))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Not enough stock");

            verify(cartItemRepository, never()).save(any());
        }
        @Test
        @DisplayName("should throw when combined quantity exceeds stock (existing item)")
        void shouldRejectInsufficientStockForCombinedQuantity(){
            product.setStock(4);
            CartItem existing = new CartItem();
            existing.setCart(cart);
            existing.setProduct(product);
            existing.setQuantity(3);

            CartItemRequestDto dto = new CartItemRequestDto();
            dto.setProductId(100L);
            dto.setQuantity(2);

            when(productRepository.findById(100L)).thenReturn(Optional.of(product));
            when(cartItemRepository.findByCartIdAndProductId(10L, 100L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> cartService.addToCart(dto, userDetails))
                    .isInstanceOf(InvalidRequestException.class);

            assertThat(existing.getQuantity()).isEqualTo(3); // unchanged
            verify(cartItemRepository, never()).save(any());
        }
        @Test
        @DisplayName("should throw when product is inactive")
        void shouldRejectInactiveProduct(){
            product.setActive(false);
            CartItemRequestDto dto = new CartItemRequestDto();
            dto.setProductId(100L);
            dto.setQuantity(1);


            when(productRepository.findById(100L)).thenReturn(Optional.of(product));
            assertThatThrownBy(() -> cartService.addToCart(dto, userDetails))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("no longer available");

            verifyNoInteractions(cartItemRepository);

        }
        @Test
        @DisplayName("should throw when product does not exist")
        void shouldThrowWhenProductNotFound() {
            CartItemRequestDto dto = new CartItemRequestDto();
            dto.setProductId(999L);
            dto.setQuantity(1);

            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addToCart(dto, userDetails))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
        @Test
        @DisplayName("should create a new cart when user has none yet")
        void shouldCreateCartWhenNoneExists() {
            when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
            Cart newCart = new Cart();
            newCart.setId(20L);
            newCart.setUser(user);
            newCart.setItems(new ArrayList<>());
            when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

            CartItemRequestDto dto = new CartItemRequestDto();
            dto.setProductId(100L);
            dto.setQuantity(1);

            when(productRepository.findById(100L)).thenReturn(Optional.of(product));
            when(cartItemRepository.findByCartIdAndProductId(20L, 100L)).thenReturn(Optional.empty());
            when(cartRepository.findById(20L)).thenReturn(Optional.of(newCart));
            when(cartMapper.toDto(newCart)).thenReturn(cartResponseDto);

            cartService.addToCart(dto, userDetails);

            verify(cartRepository).save(argThat(c -> c.getUser().equals(user)));
        }
    }


    @Test
    @DisplayName("getCart should return mapped current user's cart")
    void shouldGetCart() {
        when(cartMapper.toDto(cart)).thenReturn(cartResponseDto);

        CartResponseDto result = cartService.getCart(userDetails);

        assertThat(result).isEqualTo(cartResponseDto);
    }


    @Nested
    @DisplayName("updateCartItem")
    class UpdateCartItem {

        @Test
        @DisplayName("should update quantity when item belongs to caller's cart")
        void shouldUpdateQuantity() {
            CartItem item = new CartItem();
            item.setId(500L);
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(1);

            when(cartItemRepository.findById(500L)).thenReturn(Optional.of(item));
            when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
            when(cartMapper.toDto(cart)).thenReturn(cartResponseDto);

            cartService.updateCartItem(userDetails, 500L, 5);

            assertThat(item.getQuantity()).isEqualTo(5);
            verify(cartItemRepository).save(item);
        }

        @Test
        @DisplayName("should throw UnauthorizedActionException when item belongs to another user's cart")
        void shouldRejectUpdatingSomeoneElsesItem() {
            Cart otherCart = new Cart();
            otherCart.setId(999L);

            CartItem item = new CartItem();
            item.setId(500L);
            item.setCart(otherCart); // different cart than caller's
            item.setProduct(product);
            item.setQuantity(1);

            when(cartItemRepository.findById(500L)).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> cartService.updateCartItem(userDetails, 500L, 5))
                    .isInstanceOf(UnauthorizedActionException.class);

            verify(cartItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when requested quantity exceeds stock")
        void shouldRejectQuantityExceedingStock() {
            product.setStock(2);
            CartItem item = new CartItem();
            item.setId(500L);
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(1);

            when(cartItemRepository.findById(500L)).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> cartService.updateCartItem(userDetails, 500L, 10))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("should throw when cart item does not exist")
        void shouldThrowWhenItemMissing() {
            when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateCartItem(userDetails, 999L, 1))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }


    @Nested
    @DisplayName("removeFromCart")
    class RemoveFromCart {

        @Test
        @DisplayName("should remove item belonging to caller's cart")
        void shouldRemoveItem() {
            CartItem item = new CartItem();
            item.setId(500L);
            item.setCart(cart);
            cart.getItems().add(item);

            when(cartItemRepository.findById(500L)).thenReturn(Optional.of(item));
            when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
            when(cartMapper.toDto(cart)).thenReturn(cartResponseDto);

            cartService.removeFromCart(500L, userDetails);

            verify(cartItemRepository).delete(item);
            assertThat(cart.getItems()).doesNotContain(item);
        }

        @Test
        @DisplayName("should throw UnauthorizedActionException when removing another user's item")
        void shouldRejectRemovingSomeoneElsesItem() {
            Cart otherCart = new Cart();
            otherCart.setId(999L);
            CartItem item = new CartItem();
            item.setId(500L);
            item.setCart(otherCart);

            when(cartItemRepository.findById(500L)).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> cartService.removeFromCart(500L, userDetails))
                    .isInstanceOf(UnauthorizedActionException.class);

            verify(cartItemRepository, never()).delete(any());
        }
    }


    @Test
    @DisplayName("clearCart should empty items and save")
    void shouldClearCart() {
        CartItem item = new CartItem();
        cart.getItems().add(item);

        cartService.clearCart(userDetails);

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }


    @Nested
    @DisplayName("checkout")
    class Checkout {

        @Test
        @DisplayName("should throw InvalidRequestException when cart is empty")
        void shouldRejectEmptyCartCheckout() {
            assertThatThrownBy(() -> cartService.checkout(userDetails))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("empty cart");

            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("should create order from cart items and clear cart after success")
        void shouldCheckoutSuccessfully() {
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(2);
            cart.getItems().add(item);

            OrderResponseDto orderResponse = new OrderResponseDto();
            orderResponse.setId(777L);

            when(orderService.createOrder(any(OrderRequestDto.class), eq(userDetails)))
                    .thenReturn(orderResponse);
            when(cartRepository.save(cart)).thenReturn(cart);

            OrderResponseDto result = cartService.checkout(userDetails);

            assertThat(result.getId()).isEqualTo(777L);

            ArgumentCaptor<OrderRequestDto> captor = ArgumentCaptor.forClass(OrderRequestDto.class);
            verify(orderService).createOrder(captor.capture(), eq(userDetails));
            assertThat(captor.getValue().getItems()).hasSize(1);
            assertThat(captor.getValue().getItems().get(0).getProductId()).isEqualTo(100L);
            assertThat(captor.getValue().getItems().get(0).getQuantity()).isEqualTo(2);

            assertThat(cart.getItems()).isEmpty();
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("should not clear cart if order creation fails")
        void shouldNotClearCartWhenOrderCreationFails() {
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(1);
            cart.getItems().add(item);

            when(orderService.createOrder(any(OrderRequestDto.class), eq(userDetails)))
                    .thenThrow(new InvalidRequestException("Stock Is Not Enough For: iPhone 15 Pro"));

            assertThatThrownBy(() -> cartService.checkout(userDetails))
                    .isInstanceOf(InvalidRequestException.class);
            assertThat(cart.getItems()).hasSize(1);
            verify(cartRepository, never()).save(cart);
        }
    }
}



