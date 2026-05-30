package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.UserRequestDto;
import com.ecomerce.ecomerce_web.dtos.UserResponseDto;
import com.ecomerce.ecomerce_web.entity.Role;
import com.ecomerce.ecomerce_web.entity.User;
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
    // creating new role with specific role
    public UserResponseDto createUser(UserRequestDto userDto){
        Role role = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default role USER not found"));

      User user = toEntity(userDto,role);
      User savedUser = userRepository.save(user);
      return toDto(savedUser);
    }
    // updating user infos
    public UserResponseDto updateUser(Long id,UserRequestDto userDto){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User Not found"));
        user.setFullName(userDto.getFullName());
        user.setEmail(userDto.getEmail());
        user.setDateOfBirth(userDto.getDateOfBirth());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        User userUpdated = userRepository.save(user);
        return toDto(userUpdated);
    }
    // read or find users by id
    public UserResponseDto getById(Long id){
        User user = userRepository.findById(id).
                orElseThrow(() -> new RuntimeException("User Not found"));
        return toDto(user);
    }
    public List<UserResponseDto>getAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public  void deleteUser(Long id){
        userRepository.deleteById(id);
    }
    private User toEntity(UserRequestDto dto , Role role ){
        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setRole(role);
        user.setPassword(dto.getPassword());
        user.setDateOfBirth(dto.getDateOfBirth());
        return user;
    }
    private UserResponseDto toDto (User usr){
        UserResponseDto usrDto = new UserResponseDto();
        usrDto.setId(usr.getId());
        usrDto.setEmail(usr.getEmail());
        usrDto.setFullName(usr.getFullName());
        usrDto.setDateOfBirth(usr.getDateOfBirth());
        usrDto.setCreatedAt(LocalDateTime.now());

        if (usr.getRole() != null){
            usrDto.setRoleName(usr.getRole().getName());
        }
        return usrDto;
    }
}
