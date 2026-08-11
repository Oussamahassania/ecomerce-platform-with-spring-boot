package com.ecomerce.ecomerce_web.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Data
public class CartResponseDto {
    private Long id;
    private Long userId;
    private List<CartItemResponseDto> items;
    private BigDecimal total;
    private LocalDateTime updatedAt;
}
