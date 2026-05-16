package com.ecomerce.ecomerce_web.repository;

import com.ecomerce.ecomerce_web.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {
}
