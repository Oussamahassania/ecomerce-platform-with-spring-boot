package com.ecomerce.ecomerce_web.controller;

import com.ecomerce.ecomerce_web.entity.User;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Development-only helpers. This controller is not registered outside the "dev" profile.
 * It exists because local development has no SMTP server to deliver verification emails.
 */
@RestController
@RequestMapping("/api/dev")
@Profile("dev")
@RequiredArgsConstructor
public class DevController {

    private final UserRepository userRepository;

    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
        return ResponseEntity.ok("Account verified. You can sign in now.");
    }
}
