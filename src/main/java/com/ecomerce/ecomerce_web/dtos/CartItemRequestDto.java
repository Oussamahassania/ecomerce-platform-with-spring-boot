package com.ecomerce.ecomerce_web.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemRequestDto {
    @NotNull(message = "Product id is required")
    private Long productId;
    @NotNull(message = "Quantity is required")
    @Min(value = 1,message = "Quantity must be at least one")
    private Integer quantity;

}
