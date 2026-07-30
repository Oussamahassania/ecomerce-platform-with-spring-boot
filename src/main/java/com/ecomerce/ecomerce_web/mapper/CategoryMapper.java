package com.ecomerce.ecomerce_web.mapper;

import com.ecomerce.ecomerce_web.dtos.CategoryRequest;
import com.ecomerce.ecomerce_web.dtos.CategoryResponse;
import com.ecomerce.ecomerce_web.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    Category toEntity(CategoryRequest Dto);
    CategoryResponse toDto(Category category);

}
