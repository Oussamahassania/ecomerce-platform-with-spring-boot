package com.ecomerce.ecomerce_web.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingDto {
    private Long productId;
    private Double averageRating;
    private Long totalReviews;
}
