package com.ecomerce.ecomerce_web.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemsResponseDto {
    private Integer quantity;
    private BigDecimal price;
    private Long productId;

}
