package com.ecomerce.ecomerce_web.scheduler;

import com.ecomerce.ecomerce_web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
@Slf4j
public class UserCleanupScheduler {
    private final UserRepository userRepository;
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanUnverifiedUsers(){
        LocalDateTime limit = LocalDateTime.now().minusDays(2);
        int deleted = userRepository.deleteUnverifiedUsers(limit);
        log.info("Unverified users deleted: {}", deleted);
    }
}
