package com.ecomerce.ecomerce_web.mapper;

import com.ecomerce.ecomerce_web.dtos.PaymentResponseDto;
import com.ecomerce.ecomerce_web.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "orderId", source = "order.id")
    PaymentResponseDto toDto(Payment payment);
}
