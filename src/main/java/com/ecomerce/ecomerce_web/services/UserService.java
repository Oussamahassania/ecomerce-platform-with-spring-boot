package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.UserRequestDto;
import com.ecomerce.ecomerce_web.dtos.UserResponseDto;
import com.ecomerce.ecomerce_web.entity.Role;
import com.ecomerce.ecomerce_web.entity.User;
import com.ecomerce.ecomerce_web.mapper.UserMapper;
import com.ecomerce.ecomerce_web.repository.RoleRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class UserService {
    final private UserRepository userRepository;
    final private RoleRepository roleRepository;
    final private PasswordEncoder passwordEncoder;
    final private UserMapper userMapper;

    public UserResponseDto createUser(UserRequestDto userDto) {
        Role role = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default role USER not found"));

        User user = userMapper.toEntity(userDto, role);
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        return userMapper.toDto(userRepository.save(user));
    }

    public UserResponseDto updateUser(Long id, UserRequestDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User Not found"));

        userMapper.updateEntity(userDto, user);
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        return userMapper.toDto(userRepository.save(user));
    }

    public UserResponseDto getById(Long id) {
        return userMapper.toDto(userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User Not found")));
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
