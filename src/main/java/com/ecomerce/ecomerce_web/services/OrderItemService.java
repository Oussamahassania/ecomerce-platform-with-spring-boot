package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.OrderItemsResponseDto;
import com.ecomerce.ecomerce_web.entity.OrderItem;
import com.ecomerce.ecomerce_web.entity.Product;
import com.ecomerce.ecomerce_web.repository.OrderItemRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class OrderItemService {
    final private OrderItemRepository orderItemRepository;

    public List<OrderItemsResponseDto> getItemsByOrderId(Long orderId, UserDetails userDetails) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        if (items.isEmpty())
            throw new RuntimeException("No items found for this order");

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String orderOwnerEmail = items.get(0).getOrder().getUser().getEmail();

        if (!isAdmin && !orderOwnerEmail.equals(userDetails.getUsername())) {
            throw new RuntimeException("Access Denied: This order does not belong to you");
        }

        return items.stream().map(this::toDto).toList();
    }
    public OrderItemsResponseDto getOrderItemById(Long id, UserDetails userDetails) {
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Item Not Found"));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String orderOwnerEmail = orderItem.getOrder().getUser().getEmail();

        if (!isAdmin && !orderOwnerEmail.equals(userDetails.getUsername())) {
            throw new RuntimeException("Access Denied: This order item does not belong to you");
        }

        return toDto(orderItem);
    }
    public OrderItemsResponseDto updateQuantity(Long id, Integer quantity, UserDetails userDetails) {
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Item Not Found"));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String orderOwnerEmail = orderItem.getOrder().getUser().getEmail();

        if (!isAdmin && !orderOwnerEmail.equals(userDetails.getUsername())) {
            throw new RuntimeException("Access Denied: This order item does not belong to you");
        }

        Product product = orderItem.getProduct();
        int diff = quantity - orderItem.getQuantity();
        if (product.getStock() < diff)
            throw new RuntimeException("Stock Is Not Enough For " + product.getName());

        product.setStock(product.getStock() - diff);
        orderItem.setQuantity(quantity);
        orderItem.setPrice(product.getPrice());
        return toDto(orderItemRepository.save(orderItem));
    }
    public void removeOrderItem(Long id, UserDetails userDetails) {
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Item Not Found"));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String orderOwnerEmail = orderItem.getOrder().getUser().getEmail();

        if (!isAdmin && !orderOwnerEmail.equals(userDetails.getUsername())) {
            throw new RuntimeException("Access Denied: This order item does not belong to you");
        }

        Product product = orderItem.getProduct();
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
