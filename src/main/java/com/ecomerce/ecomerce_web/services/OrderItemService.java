package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.OrderItemsResponseDto;
import com.ecomerce.ecomerce_web.entity.OrderItem;
import com.ecomerce.ecomerce_web.entity.Product;
import com.ecomerce.ecomerce_web.exception.InvalidRequestException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.exception.UnauthorizedActionException;
import com.ecomerce.ecomerce_web.mapper.OrderItemMapper;
import com.ecomerce.ecomerce_web.repository.OrderItemRepository;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@AllArgsConstructor
public class OrderItemService {
    final private OrderItemRepository orderItemRepository;
    final private OrderItemMapper orderItemMapper;
    final private ProductRepository productRepository;
    @Transactional(readOnly = true)
    public List<OrderItemsResponseDto> getItemsByOrderId(Long orderId, UserDetails userDetails) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if (items.isEmpty())
            throw new ResourceNotFoundException("No items found for this order");

        checkOwnership(items.get(0).getOrder().getUser().getEmail(), userDetails);  // ← clean
        return items.stream().map(orderItemMapper::toDto).toList();
    }
    @Transactional(readOnly = true)
    public OrderItemsResponseDto getOrderItemById(Long id, UserDetails userDetails) {
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order Item Not Found"));

        checkOwnership(orderItem.getOrder().getUser().getEmail(), userDetails);  // ← clean
        return orderItemMapper.toDto(orderItem);
    }
    @Transactional
    public OrderItemsResponseDto updateQuantity(Long id, Integer quantity, UserDetails userDetails) {
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order Item Not Found"));

        checkOwnership(orderItem.getOrder().getUser().getEmail(), userDetails);

        Product product = orderItem.getProduct();

        int diff = quantity - orderItem.getQuantity();

        if (diff > 0 && product.getStock() < diff) {
            throw new InvalidRequestException(
                    "Stock Is Not Enough For " + product.getName()
            );
        }

        product.setStock(product.getStock() - diff);
        orderItem.setQuantity(quantity);
        orderItem.setPrice(product.getPrice());

        productRepository.save(product);
        OrderItem savedItem = orderItemRepository.save(orderItem);

        return orderItemMapper.toDto(savedItem);
    }
    @Transactional
    public void removeOrderItem(Long id, UserDetails userDetails) {
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order Item Not Found"));

        checkOwnership(orderItem.getOrder().getUser().getEmail(), userDetails);  // ← clean
        Product product = orderItem.getProduct();
        orderItemRepository.delete(orderItem);
        product.setStock(product.getStock() + orderItem.getQuantity());
        productRepository.save(product);

    }

    private void checkOwnership(String ownerEmail, UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !ownerEmail.equals(userDetails.getUsername()))
            throw new UnauthorizedActionException("Access Denied: This order item does not belong to you");
    }
}
