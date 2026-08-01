package com.ecomerce.ecomerce_web.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockUpdateRequestDto {
    @NotNull(message = "Stock is mandatory")
    @Min(value = 0,message = "Stock cannot be negative")
    private Integer stock;
}
