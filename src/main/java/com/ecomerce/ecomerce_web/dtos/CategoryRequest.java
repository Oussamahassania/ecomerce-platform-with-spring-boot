package com.ecomerce.ecomerce_web.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CategoryRequest {
    @NotBlank(message = "Category name is mandatory")
    private String name;
    private String description;
}
