package com.ecomerce.ecomerce_web.dtos;

import com.ecomerce.ecomerce_web.entity.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDto {
    private Long userId;
    private List<OrderItemRequestDto>items;
}
