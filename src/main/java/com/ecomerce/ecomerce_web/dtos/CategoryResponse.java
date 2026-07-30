package com.ecomerce.ecomerce_web.dtos;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;

}
