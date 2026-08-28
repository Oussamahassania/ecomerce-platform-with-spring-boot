package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.OrderItemRequestDto;
import com.ecomerce.ecomerce_web.dtos.OrderRequestDto;
import com.ecomerce.ecomerce_web.dtos.OrderResponseDto;
import com.ecomerce.ecomerce_web.entity.*;
import com.ecomerce.ecomerce_web.exception.InvalidRequestException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.exception.UnauthorizedActionException;
import com.ecomerce.ecomerce_web.mapper.OrderMapper;
import com.ecomerce.ecomerce_web.repository.OrderRepository;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@AllArgsConstructor
public class OrderService {
    final private OrderRepository orderRepository;
    final private ProductRepository productRepository;
    final private UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final  EmailService emailService;
    public OrderResponseDto createOrder(OrderRequestDto dto, UserDetails userDetails){
        if (dto.getItems() == null || dto.getItems().isEmpty())
            throw new InvalidRequestException("Order must contain at least one item");


        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        User user;

        if (isAdmin && dto.getUserId() != null) {
            user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
        } else {
            user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
        }

        Order order = new Order();
        order.setUser(user);
        BigDecimal total = BigDecimal.ZERO;
        for(OrderItemRequestDto itemDto : dto.getItems()){
            Product product = productRepository.findById(itemDto.getProductId()).orElseThrow(() -> new ResourceNotFoundException(
                    "Product Not Found"
            ) );
            if (product.getStock() < itemDto.getQuantity())
                throw new InvalidRequestException("Stock Is Not Enough For: " +product.getName());

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
        OrderResponseDto response =  orderMapper.toDto(orderRepository.save(order));
        emailService.sendOrderConfirmation(
                user.getEmail(),
                user.getFullName(),
                response.getId(),
                response.getItems(),
                response.getTotalAmount()
        );
        return response;
    }
    @Transactional
    public List<OrderResponseDto>getAllOrders(){
        return  orderRepository.findAll()
                .stream()
                .map(orderMapper::toDto)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<OrderResponseDto> ordersByUser(Long userId){
         return orderRepository.findByUserId(userId)
                 .stream()
                 .map(orderMapper::toDto).toList();
    }
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id, UserDetails userDetails) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order Not Found"));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // if not admin, check if the order belongs to the logged-in user
        if (!isAdmin && !order.getUser().getEmail().equals(userDetails.getUsername())) {
            throw new UnauthorizedActionException("Access Denied: This order does not belong to you");
        }

        return orderMapper.toDto(order);
    }
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId,Status status){
        Order order  =  orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order Not Found"));
        order.setStatus(status);
        OrderResponseDto response =  orderMapper.toDto(orderRepository.save(order));
        emailService.sendOrderStatusUpdate(
                order.getUser().getEmail(),
                order.getUser().getFullName(),
                orderId,
                status.name()
        );
        return response;
    }
    @Transactional
    public OrderResponseDto cancelOrder(Long orderId, UserDetails userDetails) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order Not Found"));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // check ownership
        if (!isAdmin && !order.getUser().getEmail().equals(userDetails.getUsername())) {
            throw new UnauthorizedActionException("Access Denied: This order does not belong to you");
        }

        if (order.getStatus() == Status.CANCELLED)
            throw new InvalidRequestException("Order is Already canceled");

        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
        order.setStatus(Status.CANCELLED);
        OrderResponseDto response = orderMapper.toDto(orderRepository.save(order));

        emailService.sendOrderCancellation(
                order.getUser().getEmail(),
                order.getUser().getFullName(),
                orderId
        );
        return response;
    }
    public void deleteOrder(Long orderId){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order Not Found"));
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
    @Transactional(readOnly = true)
    public List<OrderResponseDto>getOrdersByStatus(Status status){
        return orderRepository.findByStatus(status)
                .stream()
                .map(orderMapper::toDto)
                .toList();
    }

}
