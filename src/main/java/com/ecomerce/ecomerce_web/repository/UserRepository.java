package com.ecomerce.ecomerce_web.repository;

import com.ecomerce.ecomerce_web.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
}
