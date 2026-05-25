package com.ecomerce.ecomerce_web.controller;

import com.ecomerce.ecomerce_web.dtos.OrderRequestDto;
import com.ecomerce.ecomerce_web.dtos.OrderResponseDto;
import com.ecomerce.ecomerce_web.entity.Status;
import com.ecomerce.ecomerce_web.services.OrderService;
import com.ecomerce.ecomerce_web.services.ProductService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    final private ProductService productService;
    final private OrderService orderService;
    @PostMapping("/createOrder")
    public ResponseEntity<OrderResponseDto>createOrder(
            @RequestBody @Valid  OrderRequestDto dto
            ){
        OrderResponseDto response = orderService.createOrder(dto);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/AllOrders")
    public ResponseEntity<List<OrderResponseDto>> getAllOrders(){
        return ResponseEntity.ok(orderService.getAllOrders());
    }
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponseDto>>getOrdersByUser(
            @PathVariable  Long userId
    ){
       return ResponseEntity.ok(orderService.ordersByUser(userId));
    }
    @GetMapping("/order/{orderId}")
    public ResponseEntity<OrderResponseDto>getOrderById(
           @PathVariable Long orderId
    ){
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable  Long id,
            @RequestParam  Status status
    ){
      return  ResponseEntity
              .ok(orderService.updateOrderStatus(id,status));
    }
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable  Long orderId
    ){
        return ResponseEntity
                .ok(orderService.cancelOrder(orderId));
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String>delete(
           @PathVariable Long id
    ){
        orderService.deleteOrder(id);
        return ResponseEntity.ok("order deleted successfully");
    }
    @GetMapping("/countOrders")
    public ResponseEntity<Long>countOrders(){
        return ResponseEntity.ok(orderService.countOrders());
    }
    @GetMapping("/totalRevenue")
    public ResponseEntity<BigDecimal>getTotalRevenue(){
        return ResponseEntity.ok(orderService.getTotalRevenue());
    }
    @GetMapping("/ordersByStatus")
    public ResponseEntity<List<OrderResponseDto>> ordersByStatus(
            @RequestParam  Status status
    ){
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

}
