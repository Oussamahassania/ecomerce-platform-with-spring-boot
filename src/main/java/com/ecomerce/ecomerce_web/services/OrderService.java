package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.OrderItemRequestDto;
import com.ecomerce.ecomerce_web.dtos.OrderRequestDto;
import com.ecomerce.ecomerce_web.dtos.OrderResponseDto;
import com.ecomerce.ecomerce_web.entity.*;
import com.ecomerce.ecomerce_web.repository.OrderRepository;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
public class OrderService {
    final private OrderRepository orderRepository;
    final private ProductRepository productRepository;
    final private UserRepository userRepository;
    public OrderResponseDto createOrder(OrderRequestDto dto){
        User user = userRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException(
                "User Not found"
        ));
        Order order = new Order();
        order.setUser(user);
        BigDecimal total = BigDecimal.ZERO;
        for(OrderItemRequestDto itemDto : dto.getItems()){
            Product product = productRepository.findById(itemDto.getProductId()).orElseThrow(() -> new RuntimeException(
                    "Product Not Found"
            ) );
            if (product.getStock() < itemDto.getQuantity())
                throw new RuntimeException("Stock Is Not Enough For: " +product.getName());

            product.setStock(product.getStock() - itemDto.getQuantity());
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setPrice(product.getPrice());
            order.getOrderItems().add(orderItem);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
        }
        order.setTotalAmount(total);
        return toDto(orderRepository.save(order));
    }
    public OrderResponseDto toDto(Order order){
        OrderResponseDto orderResponseDto = new OrderResponseDto();
        orderResponseDto.setId(order.getId());
        orderResponseDto.setUserId(order.getUser().getId());
        orderResponseDto.setOrderDate(order.getOrderDate());
        orderResponseDto.setStatus(order.getStatus());
        orderResponseDto.setTotalAmount(order.getTotalAmount());
        return orderResponseDto;

    }
}
