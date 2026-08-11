package com.ecomerce.ecomerce_web.controller;
import com.ecomerce.ecomerce_web.dtos.CartItemRequestDto;
import com.ecomerce.ecomerce_web.dtos.CartResponseDto;
import com.ecomerce.ecomerce_web.dtos.OrderResponseDto;
import com.ecomerce.ecomerce_web.services.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    @PostMapping("/add")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CartResponseDto>addToCart(
            @RequestBody @Valid CartItemRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        return ResponseEntity.ok(
                cartService.addToCart(dto, userDetails));
    }
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CartResponseDto>getCart(
            @AuthenticationPrincipal UserDetails userDetails
    ){
        return ResponseEntity.ok(cartService.getCart(userDetails));
    }
    @PutMapping("/update/{cartItemId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CartResponseDto> updateItem(
            @PathVariable Long cartItemId,
            @RequestParam @Min(value = 1,message = "quantity must be at least 1") Integer quantity,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                cartService.updateCartItem(
                        userDetails, cartItemId, quantity));
    }
    @DeleteMapping("/remove/{cartItemId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CartResponseDto> removeItem(
            @PathVariable Long cartItemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                cartService.removeFromCart(
                        cartItemId, userDetails));
    }
    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<String> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        cartService.clearCart(userDetails);
        return ResponseEntity.ok("Cart cleared successfully");
    }
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> checkout(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                cartService.checkout(userDetails));
    }
}
