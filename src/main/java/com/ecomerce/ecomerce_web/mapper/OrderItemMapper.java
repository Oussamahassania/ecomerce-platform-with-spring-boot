package com.ecomerce.ecomerce_web.mapper;

import com.ecomerce.ecomerce_web.dtos.OrderItemsResponseDto;
import com.ecomerce.ecomerce_web.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
    @Mapping(source = "product.id",target = "productId")
    OrderItemsResponseDto toDto(OrderItem orderItem);
}
