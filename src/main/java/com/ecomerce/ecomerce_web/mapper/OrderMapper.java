package com.ecomerce.ecomerce_web.mapper;

import com.ecomerce.ecomerce_web.dtos.OrderResponseDto;
import com.ecomerce.ecomerce_web.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "orderItems", target = "items")
    OrderResponseDto toDto(Order order);
}
