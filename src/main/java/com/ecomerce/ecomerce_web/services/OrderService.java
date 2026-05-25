package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.OrderItemRequestDto;
import com.ecomerce.ecomerce_web.dtos.OrderItemsResponseDto;
import com.ecomerce.ecomerce_web.dtos.OrderRequestDto;
import com.ecomerce.ecomerce_web.dtos.OrderResponseDto;
import com.ecomerce.ecomerce_web.entity.*;
import com.ecomerce.ecomerce_web.repository.OrderRepository;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@AllArgsConstructor
public class OrderService {
    final private OrderRepository orderRepository;
    final private ProductRepository productRepository;
    final private UserRepository userRepository;
    public OrderResponseDto createOrder(OrderRequestDto dto){
        User user = userRepository.findById(dto.getUserId())
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
            productRepository.save(product);
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
    public List<OrderResponseDto>getAllOrders(){
        return  orderRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }
    public List<OrderResponseDto> ordersByUser(Long userId){
         return orderRepository.findByUserId(userId)
                 .stream()
                 .map(this::toDto).toList();
    }
    public OrderResponseDto getOrderById(Long id){
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));
        return toDto(order);
    }
    public OrderResponseDto updateOrderStatus(Long orderId,Status status){
        Order order  =  orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));
        order.setStatus(status);
        return toDto(orderRepository.save(order));
    }
    public OrderResponseDto cancelOrder(Long orderId){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));
        if(order.getStatus() == Status.CANCELLED){
            throw new RuntimeException(
                    "Order is Already canceled"
            );
        }
        for (OrderItem item : order.getOrderItems()){
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
        order.setStatus(Status.CANCELLED);
        return toDto(orderRepository.save(order));
    }
    public void deleteOrder(Long orderId){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));
        orderRepository.delete(order);
    }
    public Long countOrders(){
        return orderRepository.count();
    }
    public BigDecimal getTotalRevenue(){
        return orderRepository.findAll()
                .stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    public List<OrderResponseDto>getOrdersByStatus(Status status){
        return orderRepository.findByStatus(status)
                .stream()
                .map(this::toDto)
                .toList();
    }
    public OrderResponseDto toDto(Order order){
        OrderResponseDto orderResponseDto = new OrderResponseDto();
        orderResponseDto.setId(order.getId());
        orderResponseDto.setUserId(order.getUser().getId());
        orderResponseDto.setOrderDate(order.getOrderDate());
        orderResponseDto.setStatus(order.getStatus());
        orderResponseDto.setTotalAmount(order.getTotalAmount());
        List<OrderItemsResponseDto> itemDtos = order.getOrderItems().stream()
                .map(item -> {
                    OrderItemsResponseDto i = new OrderItemsResponseDto();
                    i.setProductId(item.getProduct().getId());
                    i.setQuantity(item.getQuantity());
                    i.setPrice(item.getPrice());
                    return i;
                }).toList();
        orderResponseDto.setItems(itemDtos);
        return orderResponseDto;

    }
}
