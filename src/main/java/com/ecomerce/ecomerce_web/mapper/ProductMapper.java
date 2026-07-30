package com.ecomerce.ecomerce_web.mapper;

import com.ecomerce.ecomerce_web.dtos.ProductRequestDto;
import com.ecomerce.ecomerce_web.dtos.ProductResponseDto;
import com.ecomerce.ecomerce_web.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "category",ignore = true)
    Product toEntity(ProductRequestDto productDto);
    @Mapping(source = "category.id",target = "categoryId")
    @Mapping(source = "category.name",target = "categoryName")
    ProductResponseDto toDto(Product product);
    @Mapping(target = "category",ignore = true)
    void updateEntity(ProductRequestDto dto, @MappingTarget Product product);
}
