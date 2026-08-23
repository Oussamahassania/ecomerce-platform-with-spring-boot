package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.PaymentRequestDto;
import com.ecomerce.ecomerce_web.dtos.PaymentResponseDto;
import com.ecomerce.ecomerce_web.entity.*;
import com.ecomerce.ecomerce_web.exception.DuplicateResourceException;
import com.ecomerce.ecomerce_web.exception.InvalidRequestException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.exception.UnauthorizedActionException;
import com.ecomerce.ecomerce_web.mapper.PaymentMapper;
import com.ecomerce.ecomerce_web.repository.OrderRepository;
import com.ecomerce.ecomerce_web.repository.PaymentRepository;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;
    private final PaymentMapper paymentMapper;
    private final PaymentGateway paymentGateway;

    @Transactional
    public PaymentResponseDto processPayment(
            Long orderId,
            PaymentRequestDto dto,
            String idempotencyKey,
            UserDetails userDetails
    ){

      if (idempotencyKey!=null){
          var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
          if (existing.isPresent()) {
              log.info("⚡ Idempotent request detected — " +
                              "returning cached payment for key: {}",
                      idempotencyKey);
              return paymentMapper.toDto(existing.get());
      }
    }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Order Not found")
                );
        boolean isAdmin = userDetails.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("ROLE_ADMIN"));
        if (!isAdmin && !order.getUser().getEmail().equals(userDetails.getUsername())){
            throw new UnauthorizedActionException("this order does not belong to you");
        }
        if (order.getStatus() == Status.CANCELLED){
            throw new InvalidRequestException("cannot pay for a canceled order");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID){
            throw new DuplicateResourceException("order is already paid");
        }
        if (order.getPaymentStatus() == PaymentStatus.REFUNDED)
            throw new InvalidRequestException(
                    "Cannot pay for a refunded order");

        String paymentRef = "PAY-" + UUID.randomUUID().toString().substring(0,8).toUpperCase();
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentReference(paymentRef);
        payment.setIdempotencyKey(idempotencyKey != null
                ? idempotencyKey
                : UUID.randomUUID().toString());
        payment.setAmount(order.getTotalAmount());

        boolean paymentSuccess = paymentGateway.charge(dto);
        if (paymentSuccess){
            payment.setStatus(PaymentStatus.PAID);
            payment.setProcessedAt(LocalDateTime.now());

            //update order
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaymentReference(paymentRef);
            order.setPaidAt(LocalDateTime.now());
            order.setStatus(Status.CONFIRMED);
            
            orderRepository.save(order);
            paymentRepository.save(payment);
            emailService.sendPaymentConfirmation(
                    order.getUser().getEmail(),
                    order.getUser().getFullName(),
                    order.getId(),
                    order.getTotalAmount(),
                    paymentRef
            );
            log.info(" Payment successful — orderId: {}," +
                    " ref: {}", orderId, paymentRef);
            
        }else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment declined");
            payment.setProcessedAt(LocalDateTime.now());
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            paymentRepository.save(payment);

            log.warn(" Payment failed — orderId: {}", orderId);
        }
        return paymentMapper.toDto(payment);
}
@Transactional(readOnly = true)
public PaymentResponseDto getPaymentStatus(Long orderId,UserDetails userDetails){
        Order order  =  orderRepository.findById(orderId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Order Not Found")
                );
    boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority()
                    .equals("ROLE_ADMIN"));
    if (!isAdmin && !order.getUser().getEmail()
            .equals(userDetails.getUsername()))
        throw new UnauthorizedActionException(
                "This order does not belong to you");
    Payment payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
            .orElseThrow(
                    () -> new ResourceNotFoundException("No Payment found for this order")
            );
    return paymentMapper.toDto(payment);

}
@Transactional
public PaymentResponseDto refund(Long orderId,UserDetails  userDetails){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Order not found")
                );
    boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority()
                    .equals("ROLE_ADMIN"));
    if (!isAdmin && !order.getUser().getEmail()
            .equals(userDetails.getUsername()))
        throw new UnauthorizedActionException(
                "Access denied");

    if (order.getPaymentStatus() != PaymentStatus.PAID)
        throw new InvalidRequestException(
                "Only PAID orders can be refunded");

    for (OrderItem item:order.getOrderItems()){
        Product product = item.getProduct();
        product.setStock(product.getStock() + item.getQuantity());
        productRepository.save(product);
    }

    // update order
    order.setPaymentStatus(PaymentStatus.REFUNDED);
    order.setStatus(Status.CANCELLED);
    orderRepository.save(order);
    Payment refund = new Payment();
    refund.setOrder(order);
    refund.setPaymentReference("REF-"+ UUID.randomUUID().toString().substring(0,8).toUpperCase());
    refund.setIdempotencyKey(UUID.randomUUID().toString());
    refund.setAmount(order.getTotalAmount());
    refund.setStatus(PaymentStatus.REFUNDED);
    refund.setProcessedAt(LocalDateTime.now());
    paymentRepository.save(refund);
    //send refund email
    emailService.sendRefundConfirmation(
            order.getUser().getEmail(),
            order.getUser().getFullName(),
            order.getId(),
            order.getTotalAmount()
    );
    log.info(" Refund processed — orderId: {}", orderId);
    return paymentMapper.toDto(refund);
}

}
