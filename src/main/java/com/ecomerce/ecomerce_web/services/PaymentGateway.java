package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.PaymentRequestDto;
import org.springframework.stereotype.Component;

@Component
public class PaymentGateway {
    public boolean charge(PaymentRequestDto dto) {
        return Math.random() > 0.1;
    }
}