package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.*;
import com.ecomerce.ecomerce_web.entity.Cart;
import com.ecomerce.ecomerce_web.entity.CartItem;
import com.ecomerce.ecomerce_web.entity.Product;
import com.ecomerce.ecomerce_web.entity.User;
import com.ecomerce.ecomerce_web.exception.InvalidRequestException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.exception.UnauthorizedActionException;
import com.ecomerce.ecomerce_web.mapper.CartMapper;
import com.ecomerce.ecomerce_web.repository.CartItemRepository;
import com.ecomerce.ecomerce_web.repository.CartRepository;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final CartMapper cartMapper;
    private User getUser(UserDetails userDetails){
     return userRepository.findByEmail(userDetails.getUsername())
             .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
    }
    private Cart getOrCreateCart(User user){
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }
    private void checkCartItemOwnership(CartItem cartItem,Cart cart){
        if (!cartItem.getCart().getId().equals(cart.getId())){
            throw new UnauthorizedActionException("this item does not belong to your cart");
        }
    }
    @Transactional
    public CartResponseDto addToCart(CartItemRequestDto cartItemRequestDto,UserDetails userDetails){
        User user  = getUser(userDetails);
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(cartItemRequestDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("product not found"));
        if (!product.isActive()){
            throw new InvalidRequestException("Product is no longer available");
        }

        cartItemRepository.findByCartIdAndProductId(cart.getId(),cartItemRequestDto.getProductId()).ifPresentOrElse(existing -> {
            int newQty = existing.getQuantity() + cartItemRequestDto.getQuantity();
            if (product.getStock() < newQty){
                throw new InvalidRequestException(
                        "Not enough stock for: "
                                + product.getName());
            }
            existing.setQuantity(newQty);
            cartItemRepository.save(existing);
        },
                () -> {
            if (product.getStock() < cartItemRequestDto.getQuantity()){
                throw new InvalidRequestException("Not enough stock for: "+product.getName());
            }
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(cartItemRequestDto.getQuantity());
            item.setPriceAtAdding(product.getPrice());
            cartItemRepository.save(item);
                }
                );
        log.info("🛒 [{}] Added product {} to cart",
                userDetails.getUsername(), cartItemRequestDto.getProductId());
        return cartMapper.toDto(cartRepository.findById(cart.getId())
                .orElseThrow());
    }
    @Transactional(readOnly = true)
    public CartResponseDto getCart(UserDetails userDetails){
        User user = getUser(userDetails);
        Cart cart  = getOrCreateCart(user);
        return cartMapper.toDto(cart);
    }
    @Transactional
    public CartResponseDto updateCartItem(UserDetails userDetails,Long cartItemId,Integer quantity){
        User user = getUser(userDetails);
        Cart cart = getOrCreateCart(user);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart Item Not found"));
        checkCartItemOwnership(cartItem,cart);
        if (cartItem.getProduct().getStock() < quantity){
            throw new InvalidRequestException(
                    "Stock not enough for: "+cartItem.getProduct().getName()
            );
        }
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
        return cartMapper.toDto(cartRepository.findById(cart.getId())
                .orElseThrow());

    }
    @Transactional
    public CartResponseDto removeFromCart(Long cartItemId,UserDetails userDetails){
        User user = getUser(userDetails);
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found"));
        checkCartItemOwnership(item, cart);
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        log.info("🗑️ [{}] Removed cart item {}",
                userDetails.getUsername(), cartItemId);

        return cartMapper.toDto(cartRepository.findById(cart.getId())
                .orElseThrow());

    }
    @Transactional(readOnly = true)
    public void clearCart(UserDetails userDetails){
        User user = getUser(userDetails);
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
        log.info("🗑️ [{}] Cart cleared", userDetails.getUsername());
    }
    @Transactional
    public OrderResponseDto checkout(UserDetails userDetails){
        User user = getUser(userDetails);
        Cart cart = getOrCreateCart(user);
        if (cart.getItems().isEmpty())
            throw new InvalidRequestException("Cannot checkout with an empty cart");

        OrderRequestDto orderRequestDto = new OrderRequestDto();
        List<OrderItemRequestDto>orderItems = cart.getItems()
                .stream()
                .map(item -> {
                    OrderItemRequestDto orderItem = new OrderItemRequestDto();
                    orderItem.setProductId(item.getProduct().getId());
                    orderItem.setQuantity(item.getQuantity());
                    return orderItem;
                }).toList();
        orderRequestDto.setItems(orderItems);
        OrderResponseDto response =
                orderService.createOrder(orderRequestDto, userDetails);
        clearCart(userDetails);
        log.info("✅ [{}] Checkout successful — orderId: {}",
                userDetails.getUsername(), response.getId());

        return response;

    }
}
