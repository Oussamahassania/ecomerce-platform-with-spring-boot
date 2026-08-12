package com.ecomerce.ecomerce_web.dtos;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class ReviewResponseDto {
    private Long id;
    private Long productId;
    private String productName;
    private Long userId;
    private String userFullName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
