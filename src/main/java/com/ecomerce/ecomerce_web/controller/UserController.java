package com.ecomerce.ecomerce_web.controller;

import com.ecomerce.ecomerce_web.dtos.UserRequestDto;
import com.ecomerce.ecomerce_web.dtos.UserResponseDto;
import com.ecomerce.ecomerce_web.services.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {
    final private UserService userService;
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> createUser(
           @RequestBody @Valid UserRequestDto userDto
    ){
        UserResponseDto response =  userService.createUser(userDto);
        return ResponseEntity.ok(response);
    }
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> updateUserInfo(
            @PathVariable  Long id,
            @RequestBody @Valid UserRequestDto userDto){
        UserResponseDto response = userService.updateUser(id,userDto);
        return  ResponseEntity.ok(response);
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto>displayById(
            @PathVariable Long id){
        UserResponseDto userResponseDto = userService.getById(id);
        return ResponseEntity.ok(userResponseDto);
    }
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDto>>displayAll(){
        List<UserResponseDto>responses = userService.getAllUsers();
        return ResponseEntity.ok(responses);
    }
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String>delete(
            @PathVariable Long id){
        userService.deleteUser(id);
        return ResponseEntity.ok("user deleted successfully");
    }
}
