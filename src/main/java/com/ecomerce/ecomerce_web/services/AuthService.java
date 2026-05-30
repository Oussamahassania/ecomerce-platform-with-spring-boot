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

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private  final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;



    public AuthResponse register(RegisterRequest request){
        Role userRole  = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role Not Found In Db"));
      User user  = User.builder()
              .fullName(request.getFullName())
              .email(request.getEmail())
              .password(passwordEncoder.encode(request.getPassword()))
              .role(userRole)
              .dateOfBirth(request.getDateOfBirth())
              .build();
      userRepository.save(user);
      String token = jwtUtils.generateToken(user);
      return new AuthResponse(token,null);

    }
    public AuthResponse login(LoginRequest request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword())
        );
        User user =  userRepository.findByEmail(request.getEmail()).
                orElseThrow(() -> new RuntimeException("User Not Found"));
        String token = jwtUtils.generateToken(user);
        return new AuthResponse(token,null);
    }
}
