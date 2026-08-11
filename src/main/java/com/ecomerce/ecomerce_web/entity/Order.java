package com.ecomerce.ecomerce_web.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING)
    private Status status;
    @OneToMany(mappedBy = "order",cascade = CascadeType.ALL)
    private List<OrderItem>orderItems = new ArrayList<>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    private String paymentReference;
    private LocalDateTime paidAt;
    @Version
    @Column(nullable = false)
    private Long version;
    @PrePersist
    protected void onCreate(){
        orderDate = LocalDateTime.now();
        status= Status.PENDING;
    }
}
