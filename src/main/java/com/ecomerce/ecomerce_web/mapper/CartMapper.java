package com.ecomerce.ecomerce_web.mapper;

import com.ecomerce.ecomerce_web.dtos.CartItemResponseDto;
import com.ecomerce.ecomerce_web.dtos.CartResponseDto;
import com.ecomerce.ecomerce_web.entity.Cart;
import com.ecomerce.ecomerce_web.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "total", expression = "java(cart.getTotal())")
    CartResponseDto toDto(Cart cart);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productImage", source = "product.img_url")
    @Mapping(target = "subtotal",
            expression = "java(computeSubtotal(item))")
    CartItemResponseDto toDto(CartItem item);

    List<CartItemResponseDto> toDtoList(List<CartItem> items);

    default BigDecimal computeSubtotal(CartItem item) {
        return item.getPriceAtAdding()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}