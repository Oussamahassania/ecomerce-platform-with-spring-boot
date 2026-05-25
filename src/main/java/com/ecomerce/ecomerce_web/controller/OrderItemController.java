package com.ecomerce.ecomerce_web.controller;

import com.ecomerce.ecomerce_web.dtos.OrderItemsResponseDto;
import com.ecomerce.ecomerce_web.services.OrderItemService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orderItem")
@AllArgsConstructor
public class OrderItemController {

    final private OrderItemService orderItemService;
    @GetMapping("/itemByOrderId/{orderId}")
    public ResponseEntity<List<OrderItemsResponseDto>>itemByOrderId(
            @PathVariable  Long orderId){
        return ResponseEntity.ok(orderItemService.getItemsByOrderId(orderId));

    }
    @GetMapping("/orderItemById/{orderItemId}")
    public ResponseEntity<OrderItemsResponseDto>orderItemById(
            @PathVariable  Long orderItemId){
        return ResponseEntity.ok(orderItemService.getOrderItemById(orderItemId));
    }
    @PutMapping("/updateQuantity/{id}")
    public ResponseEntity<OrderItemsResponseDto>updateQuantity(
           @PathVariable Long id,
            @RequestParam Integer quantity
    ){
        return ResponseEntity.ok(orderItemService.updateQuantity(id,quantity));
    }
    @DeleteMapping("/removeOrderItem/{id}")
    public ResponseEntity<String>removeOrderItem(
            @PathVariable Long id
    ){
        orderItemService.removeOrderItem(id);
        return ResponseEntity.ok("OrderItem Removed Successfully");
    }
}
