package com.ecomerce.ecomerce_web.repository;

import com.ecomerce.ecomerce_web.entity.Order;
import com.ecomerce.ecomerce_web.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {
    List<Order>findByUserId(Long userId);
    List<Order>findByStatus(Status status);
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.orderDate < :date")
    List<Order>findOldPendingOrders(@Param("date") LocalDateTime date);
}
