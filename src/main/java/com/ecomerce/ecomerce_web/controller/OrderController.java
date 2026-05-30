package com.ecomerce.ecomerce_web.controller;

import com.ecomerce.ecomerce_web.dtos.OrderRequestDto;
import com.ecomerce.ecomerce_web.dtos.OrderResponseDto;
import com.ecomerce.ecomerce_web.entity.Status;
import com.ecomerce.ecomerce_web.services.OrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    final private OrderService orderService;
    @PostMapping("/createOrder")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> createOrder(
            @RequestBody @Valid OrderRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails  // ← add this
    ) {
        return ResponseEntity.ok(orderService.createOrder(dto, userDetails));
    }
    @GetMapping("/AllOrders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponseDto>> getAllOrders(){
        return ResponseEntity.ok(orderService.getAllOrders());
    }
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponseDto>>getOrdersByUser(
            @PathVariable  Long userId
    ){
       return ResponseEntity.ok(orderService.ordersByUser(userId));
    }
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(orderService.getOrderById(orderId, userDetails));
    }
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable  Long id,
            @RequestParam  Status status
    ){
      return  ResponseEntity
              .ok(orderService.updateOrderStatus(id,status));
    }
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails  // ← add this
    ) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, userDetails));
    }
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String>delete(
           @PathVariable Long id
    ){
        orderService.deleteOrder(id);
        return ResponseEntity.ok("order deleted successfully");
    }
    @GetMapping("/countOrders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long>countOrders(){
        return ResponseEntity.ok(orderService.countOrders());
    }
    @GetMapping("/totalRevenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BigDecimal>getTotalRevenue(){
        return ResponseEntity.ok(orderService.getTotalRevenue());
    }
    @GetMapping("/ordersByStatus")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponseDto>> ordersByStatus(
            @RequestParam  Status status
    ){
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

}
