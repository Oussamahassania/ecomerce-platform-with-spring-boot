package com.ecomerce.ecomerce_web.scheduler;

import com.ecomerce.ecomerce_web.entity.Order;
import com.ecomerce.ecomerce_web.repository.OrderRepository;
import com.ecomerce.ecomerce_web.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderReminderScheduler {

    private final OrderRepository orderRepository;
    private final EmailService emailService;
    @Scheduled(cron = "0 0 * * * *")
    public void sendPendingOrderReminders(){
        LocalDateTime limits = LocalDateTime.now().minusDays(2);
        List<Order> orders = orderRepository.findOldPendingOrders(limits);
         for (Order order : orders){
             emailService.sendOrderStatusUpdate(
                     order.getUser().getEmail(),
                     order.getUser().getFullName(),
                     order.getId(),
                     "REMINDER: Your order is still pending"
             );
         }
        log.info(" Pending order reminders sent: {}", orders.size());
    }
}
