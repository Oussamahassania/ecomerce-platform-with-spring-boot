package com.ecomerce.ecomerce_web.controller;

import com.ecomerce.ecomerce_web.dtos.OrderItemsResponseDto;
import com.ecomerce.ecomerce_web.services.OrderItemService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orderItem")
@AllArgsConstructor
public class OrderItemController {

    final private OrderItemService orderItemService;
    @GetMapping("/itemByOrderId/{orderId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<OrderItemsResponseDto>> itemByOrderId(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails  // ← add this
    ) {
        return ResponseEntity.ok(orderItemService.getItemsByOrderId(orderId, userDetails));
    }
    @GetMapping("/orderItemById/{orderItemId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<OrderItemsResponseDto> orderItemById(
            @PathVariable Long orderItemId,
            @AuthenticationPrincipal UserDetails userDetails  // ← add this
    ) {
        return ResponseEntity.ok(orderItemService.getOrderItemById(orderItemId, userDetails));
    }
    @PutMapping("/updateQuantity/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<OrderItemsResponseDto> updateQuantity(
            @PathVariable Long id,
            @RequestParam Integer quantity,
            @AuthenticationPrincipal UserDetails userDetails  // ← add this
    ) {
        return ResponseEntity.ok(orderItemService.updateQuantity(id, quantity, userDetails));
    }
    @DeleteMapping("/removeOrderItem/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<String> removeOrderItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails  // ← add this
    ) {
        orderItemService.removeOrderItem(id, userDetails);
        return ResponseEntity.ok("OrderItem Removed Successfully");
    }
}
