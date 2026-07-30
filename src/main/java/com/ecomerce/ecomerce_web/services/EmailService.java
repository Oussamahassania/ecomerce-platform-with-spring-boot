package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.OrderItemsResponseDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    @Value("${app.base-url}")
    private String baseUrl;


    @Async("emailExecuter")
    public void sendVerificationEmail(String to,String fullName,String token){
        Context context = new Context();
        context.setVariable("fullName",fullName);
        context.setVariable(
                "verificationUrl",
                baseUrl + "/api/auth/verifiy?token=" + token
        );

        sendEmail(to," Verify Your Email — EcommerceWeb",
                "emails/welcome", context);
    }
    @Async("emailExecuter")
    public void sendOrderConfirmation(String to, String fullName, Long orderId,
                                       List<OrderItemsResponseDto> items,
                                       BigDecimal totalAmount){
        Context context = new Context();
        context.setVariable("fullName",fullName);
        context.setVariable("orderId",orderId);
        context.setVariable("items",items);
        context.setVariable("totalAmount",totalAmount);

         sendEmail(to, " Order Confirmed #" + orderId,
                 "emails/order-confirmation", context);

     }
    @Async("emailExecuter")
    public void sendOrderStatusUpdate(
             String to, String fullName,
             Long orderId,String status){
        Context context = new Context();
        context.setVariable("fullName",fullName);
        context.setVariable("orderId",orderId);
        context.setVariable("status",status);

         sendEmail(to, " Order #" + orderId + " Status Updated",
                 "emails/order-status", context);
     }
    @Async("emailExecuter")
    public void sendOrderCancellation(String to,String fullName,Long orderId){
        Context context = new Context();
        context.setVariable("fullName",fullName);
        context.setVariable("orderId",orderId);

         sendEmail(to, " Order #" + orderId + " Cancelled",
                 "emails/order-canceled", context);
     }
    private void sendEmail(
            String to, String subject,
            String template, Context context)
    {
        log.info(
                "Thread: {}",
                Thread.currentThread().getName()
        );

        try {
         String html = templateEngine.process(template,context);
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,true,"UTF-8");
            helper.setFrom("noreply@sandbox.mailtrap.io");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            javaMailSender.send(message);
            log.info(" Email sent to {} — subject: {}", to, subject);
        }catch (MessagingException e){
            log.error(" Failed to send email to {} — {}", to, e.getMessage());
        }
    }
}
