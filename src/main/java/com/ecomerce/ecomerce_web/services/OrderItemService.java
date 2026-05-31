package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.OrderItemsResponseDto;
import com.ecomerce.ecomerce_web.entity.OrderItem;
import com.ecomerce.ecomerce_web.entity.Product;
import com.ecomerce.ecomerce_web.mapper.OrderItemMapper;
import com.ecomerce.ecomerce_web.repository.OrderItemRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
@AllArgsConstructor
public class OrderItemService {
    final private OrderItemRepository orderItemRepository;
    final private OrderItemMapper orderItemMapper;

    public List<OrderItemsResponseDto> getItemsByOrderId(Long orderId, UserDetails userDetails) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if (items.isEmpty())
            throw new RuntimeException("No items found for this order");

        checkOwnership(items.get(0).getOrder().getUser().getEmail(), userDetails);  // ← clean
        return items.stream().map(orderItemMapper::toDto).toList();
    }

    public OrderItemsResponseDto getOrderItemById(Long id, UserDetails userDetails) {
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Item Not Found"));

        checkOwnership(orderItem.getOrder().getUser().getEmail(), userDetails);  // ← clean
        return orderItemMapper.toDto(orderItem);
    }

    public OrderItemsResponseDto updateQuantity(Long id, Integer quantity, UserDetails userDetails) {
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Item Not Found"));

        checkOwnership(orderItem.getOrder().getUser().getEmail(), userDetails);  // ← clean

        Product product = orderItem.getProduct();
        int diff = quantity - orderItem.getQuantity();
        if (product.getStock() < diff)
            throw new RuntimeException("Stock Is Not Enough For " + product.getName());

        product.setStock(product.getStock() - diff);
        orderItem.setQuantity(quantity);
        orderItem.setPrice(product.getPrice());
        return orderItemMapper.toDto(orderItemRepository.save(orderItem));
    }

    public void removeOrderItem(Long id, UserDetails userDetails) {
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Item Not Found"));

        checkOwnership(orderItem.getOrder().getUser().getEmail(), userDetails);  // ← clean

        Product product = orderItem.getProduct();
        product.setStock(product.getStock() + orderItem.getQuantity());
        orderItemRepository.delete(orderItem);
    }

    private void checkOwnership(String ownerEmail, UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !ownerEmail.equals(userDetails.getUsername()))
            throw new RuntimeException("Access Denied: This order item does not belong to you");
    }
}
