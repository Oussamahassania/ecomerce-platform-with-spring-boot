package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.OrderItemsResponseDto;
import com.ecomerce.ecomerce_web.entity.OrderItem;
import com.ecomerce.ecomerce_web.entity.Product;
import com.ecomerce.ecomerce_web.repository.OrderItemRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class OrderItemService {
    final private OrderItemRepository orderItemRepository;

    public List<OrderItemsResponseDto>getItemsByOrderId(Long orderId){
        return orderItemRepository.findByOrderId(orderId).stream().map(this::toDto).toList();
    }
    public OrderItemsResponseDto getOrderItemById(Long id){
        OrderItem orderItem = orderItemRepository.findById(id).orElseThrow(() -> new RuntimeException("Order Item Not Found"));
        return toDto(orderItem);
    }
    public OrderItemsResponseDto updateQuantity(Long id,Integer quantity){
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Item Not Found"));
        Product product =  orderItem.getProduct();
        int diff = quantity - orderItem.getQuantity();
        if (product.getStock() < diff){
            throw new RuntimeException("Stock Is Not Enough For "+ product.getName());
        }
        product.setStock(product.getStock() - diff);
        orderItem.setQuantity(quantity);
        orderItem.setPrice(product.getPrice());
        return toDto(orderItemRepository.save(orderItem));
    }
    public void removeOrderItem(Long id){
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Item Not Found "));
        Product product  =  orderItem.getProduct();
        product.setStock(product.getStock() + orderItem.getQuantity());
        orderItemRepository.delete(orderItem);
    }

    private OrderItemsResponseDto toDto(OrderItem orderItem) {
        OrderItemsResponseDto dto = new OrderItemsResponseDto();
        dto.setPrice(orderItem.getPrice());
        dto.setQuantity(orderItem.getQuantity());
        dto.setProductId(orderItem.getProduct().getId());
        return dto;
    }

}
