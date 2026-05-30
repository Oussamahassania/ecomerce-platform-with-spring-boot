package com.ecomerce.ecomerce_web.controller;

import com.ecomerce.ecomerce_web.services.RoleService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@AllArgsConstructor
public class RoleController {
    final private RoleService roleService;

    @PutMapping("/change")
    @PreAuthorize("hasRole('ADMIN')")                  // ← only ADMIN changes roles
    public ResponseEntity<String> changeRole(
            @RequestParam Long userId,
            @RequestParam String roleName) {
        roleService.changeRole(userId, roleName);
        return ResponseEntity.ok("Role updated successfully");
    }
}
