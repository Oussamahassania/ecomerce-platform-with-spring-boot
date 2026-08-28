package com.ecomerce.ecomerce_web.config;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;


@TestConfiguration
public class TestMailConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        Session session = Session.getInstance(new Properties());
        Mockito.when(mailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(session));
        return mailSender;
    }
}