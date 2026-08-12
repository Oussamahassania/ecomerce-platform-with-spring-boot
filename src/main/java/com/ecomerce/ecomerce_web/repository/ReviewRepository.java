package com.ecomerce.ecomerce_web.repository;

import com.ecomerce.ecomerce_web.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review,Long> {
    List<Review>findByProductId(Long productId);
    Optional<Review>findByUserIdAndProductId(Long userId,Long productId);
    @Query("""
        SELECT COUNT(oi) > 0
        FROM OrderItem oi
        WHERE oi.order.user.id = :userId
        AND oi.product.id = :productId
        AND oi.order.status != 'CANCELLED'
        """)
    boolean hasUserOrderedProduct(
            @Param("userId") Long userId,
            @Param("productId") Long productId
    );
    @Query("""
        SELECT AVG(r.rating)
        FROM Review r
        WHERE r.product.id = :productId
        """)
    Double findAverageRatingByProductId(
            @Param("productId") Long productId);
    Long countByProductId(Long productId);
}
