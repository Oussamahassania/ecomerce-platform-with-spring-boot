package com.ecomerce.ecomerce_web.controller;

import com.ecomerce.ecomerce_web.dtos.PaymentRequestDto;
import com.ecomerce.ecomerce_web.dtos.PaymentResponseDto;
import com.ecomerce.ecomerce_web.services.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;


    @PostMapping("/pay/{orderId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto>pay(
            @PathVariable Long orderId,
            @Valid  @RequestBody PaymentRequestDto dto,
            @RequestHeader(value = "idempotency-key",required = false) String idempotencyKey,
            @AuthenticationPrincipal UserDetails userDetails
            ){
      return ResponseEntity.ok(paymentService.processPayment(orderId,dto,idempotencyKey,userDetails));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto>getStatus(
           @PathVariable Long orderId,
            @AuthenticationPrincipal  UserDetails userDetails){
     return ResponseEntity.ok(paymentService.getPaymentStatus(orderId,userDetails));
    }

    @PostMapping("/refund/{orderId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto> refund(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                paymentService.refund(orderId, userDetails));
    }
}
