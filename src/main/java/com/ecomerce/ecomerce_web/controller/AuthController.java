package com.ecomerce.ecomerce_web.controller;

import com.ecomerce.ecomerce_web.dtos.AuthResponse;
import com.ecomerce.ecomerce_web.dtos.LoginRequest;
import com.ecomerce.ecomerce_web.dtos.RegisterRequest;
import com.ecomerce.ecomerce_web.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/registration")
    public ResponseEntity<AuthResponse>register(
            @RequestBody @Valid RegisterRequest request
    ){
        return ResponseEntity.ok(authService.register(request));
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse>login(
            @Valid @RequestBody LoginRequest request
    ){
        return ResponseEntity.ok(authService.login(request));
    }
}
