package com.ecomerce.ecomerce_web.dtos;
import com.ecomerce.ecomerce_web.entity.PaymentStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class PaymentResponseDto {
    private Long id;
    private Long orderId;
    private String paymentReference;
    private BigDecimal amount;
    private PaymentStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
