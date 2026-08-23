package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.entity.Role;
import com.ecomerce.ecomerce_web.entity.User;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.repository.RoleRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService(userRepository, roleRepository);
    }

    @Test
    @DisplayName("should assign new role to existing user")
    void shouldChangeRole() {
        User user = new User();
        user.setId(1L);
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));

        roleService.changeRole(1L, "ROLE_ADMIN");

        assertThat(user.getRole()).isEqualTo(adminRole);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("should throw when user does not exist")
    void shouldThrowWhenUserMissing() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.changeRole(999L, "ROLE_ADMIN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw when role does not exist")
    void shouldThrowWhenRoleMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(roleRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.changeRole(1L, "NONEXISTENT"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }
}