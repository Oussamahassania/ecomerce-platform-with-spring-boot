package com.ecomerce.ecomerce_web.mapper;

import com.ecomerce.ecomerce_web.dtos.ProductRequestDto;
import com.ecomerce.ecomerce_web.dtos.ProductResponseDto;
import com.ecomerce.ecomerce_web.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toEntity(ProductRequestDto productDto);
    ProductResponseDto toDto(Product product);
    void updateEntity(ProductRequestDto dto, @MappingTarget Product product);
}
