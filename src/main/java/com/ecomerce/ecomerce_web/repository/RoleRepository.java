package com.ecomerce.ecomerce_web.repository;

import com.ecomerce.ecomerce_web.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role,Long> {

    Optional<Role>findByName(String name);
}
