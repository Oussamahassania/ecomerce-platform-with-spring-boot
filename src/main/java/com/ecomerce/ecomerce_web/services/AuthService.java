package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.AuthResponse;
import com.ecomerce.ecomerce_web.dtos.LoginRequest;
import com.ecomerce.ecomerce_web.dtos.RegisterRequest;
import com.ecomerce.ecomerce_web.entity.Role;
import com.ecomerce.ecomerce_web.entity.User;
import com.ecomerce.ecomerce_web.repository.RoleRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import com.ecomerce.ecomerce_web.security.JwtUtils;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private  final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final EmailService emailService;

    public String register(RegisterRequest request){

        if (userRepository.findByEmail(request.getEmail()).isPresent())
            throw new RuntimeException("Email already in use");

        Role userRole  = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role Not Found In Db"));
        String token_gen = UUID.randomUUID().toString();
        User user  = User.builder()
              .fullName(request.getFullName())
              .email(request.getEmail())
              .password(passwordEncoder.encode(request.getPassword()))
              .role(userRole)
                .emailVerified(false)
                .verificationToken(token_gen)
              .dateOfBirth(request.getDateOfBirth())
              .build();
      userRepository.save(user);

      emailService.sendVerificationEmail(
              user.getEmail(),
              user.getFullName(),
              user.getVerificationToken()
      );

        return "Registration successful! Please check your email to verify your account.";
    }
    public AuthResponse login(LoginRequest request){
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User Not Found"));
        if (!user.isEmailVerified())
            throw new RuntimeException("Please verify your email before logging in");
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        String token  = jwtUtils.generateToken(user);
        return new AuthResponse(token,null);
    }
    public String verifyEmail(String token){
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification token"));
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        return "Email verified successfully! You can now login.";

    }
}
