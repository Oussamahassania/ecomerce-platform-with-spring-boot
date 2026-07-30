package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.entity.Role;
import com.ecomerce.ecomerce_web.entity.User;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.repository.RoleRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RoleService {
    final private UserRepository userRepository;
    final private RoleRepository roleRepository;
    public void changeRole(Long userId,String rolaName){
        User user = userRepository.findById(userId).
                orElseThrow(() -> new ResourceNotFoundException("User Not found"));

        Role role = roleRepository.findByName(rolaName).
                orElseThrow(() ->  new ResourceNotFoundException("Role Not found"));
        user.setRole(role);
        userRepository.save(user);
    }
}
