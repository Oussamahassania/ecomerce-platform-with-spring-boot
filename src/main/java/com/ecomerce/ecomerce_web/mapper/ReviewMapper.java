package com.ecomerce.ecomerce_web.mapper;

import com.ecomerce.ecomerce_web.dtos.ReviewResponseDto;
import com.ecomerce.ecomerce_web.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userFullName", source = "user.fullName")
    ReviewResponseDto toDto(Review review);

    List<ReviewResponseDto> toDtoList(List<Review> reviews);
}
