package com.ecomerce.ecomerce_web.scheduler;

import com.ecomerce.ecomerce_web.dtos.ProductResponseDto;
import com.ecomerce.ecomerce_web.entity.Product;
import com.ecomerce.ecomerce_web.entity.User;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import com.ecomerce.ecomerce_web.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockAlertScheduler {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    @Transactional(readOnly = true)
    @Scheduled(cron = "0 0 9 * * *")
    public void sendLowStockAlerts(){

        List<Product>lowStock = productRepository.findByStockLessThanAndActiveTrue(10);
        if (lowStock.isEmpty())return;
        List<User>admins = userRepository.findByRole_NameIgnoreCase("ADMIN");
        admins.forEach(admin -> emailService.sendLowStockAlert(admin.getEmail(),admin.getFullName(),lowStock));

    }
}
