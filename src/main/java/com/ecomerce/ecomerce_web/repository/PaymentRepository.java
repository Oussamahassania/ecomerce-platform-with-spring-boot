package com.ecomerce.ecomerce_web.repository;

import com.ecomerce.ecomerce_web.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface PaymentRepository extends JpaRepository<Payment,Long> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByOrderId(Long orderId);

    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(
            Long orderId);
}
