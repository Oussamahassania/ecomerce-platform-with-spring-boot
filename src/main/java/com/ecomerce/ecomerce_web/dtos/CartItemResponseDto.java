package com.ecomerce.ecomerce_web.dtos;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class CartItemResponseDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private BigDecimal priceAtAdding;
    private BigDecimal subtotal;
}
