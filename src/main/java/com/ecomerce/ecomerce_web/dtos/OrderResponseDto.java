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
public class OrderResponseDto {
  private Long id;
  private Long userId;
  private Status status;
  private LocalDateTime orderDate;
  private BigDecimal totalAmount;
  private List<OrderItemsResponseDto> items;
}
