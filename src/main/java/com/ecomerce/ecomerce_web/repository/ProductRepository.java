package com.ecomerce.ecomerce_web.repository;

import com.ecomerce.ecomerce_web.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByActiveTrue();
    Page<Product>findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);
    List<Product> findByStockLessThanAndActiveTrue(Integer threshold);


}
