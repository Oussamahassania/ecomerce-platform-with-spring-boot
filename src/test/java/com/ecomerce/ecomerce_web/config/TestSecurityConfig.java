// src/test/java/com/ecomerce/ecomerce_web/config/TestSecurityConfig.java
package com.ecomerce.ecomerce_web.config;

import com.ecomerce.ecomerce_web.security.JwtAuthFilter;
import com.ecomerce.ecomerce_web.security.JwtUtils;
import com.ecomerce.ecomerce_web.security.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    JwtAuthFilter jwtAuthFilter(JwtUtils jwtUtils, UserDetailsServiceImpl uds) {
        return new JwtAuthFilter(jwtUtils, uds) {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain)
                    throws ServletException, IOException {
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    CacheManager cacheManager() {
        return new NoOpCacheManager();
    }

    @Bean
    tools.jackson.databind.ObjectMapper objectMapper() {
        return new tools.jackson.databind.ObjectMapper();
    }
}