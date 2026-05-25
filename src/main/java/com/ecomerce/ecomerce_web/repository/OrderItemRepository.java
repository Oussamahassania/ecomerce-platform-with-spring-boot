package com.ecomerce.ecomerce_web.repository;

import com.ecomerce.ecomerce_web.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {
    List<OrderItem>findByOrderId(Long orderId);
}
