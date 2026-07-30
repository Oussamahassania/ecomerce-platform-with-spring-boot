package com.ecomerce.ecomerce_web.dtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequestDto {
    @NotBlank(message = "name is mandatory")
    private String name;
    @NotBlank(message = "description is mandatory")
    @Size(min = 20,max = 200)
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String img_url;
    private Long categoryId;

}
