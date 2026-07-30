package com.ecomerce.ecomerce_web.controller;

import com.ecomerce.ecomerce_web.dtos.CategoryRequest;
import com.ecomerce.ecomerce_web.dtos.CategoryResponse;
import com.ecomerce.ecomerce_web.dtos.ProductResponseDto;
import com.ecomerce.ecomerce_web.services.CategoryService;
import com.ecomerce.ecomerce_web.services.ProductService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@AllArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final ProductService productService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse>create(
            @Valid @RequestBody CategoryRequest request
    ){
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }
    @GetMapping
    public ResponseEntity<List<CategoryResponse>>getAll(){
      return ResponseEntity.ok(categoryService.getAll());
    }
    @GetMapping("/{id}/products")
    public ResponseEntity<Page<ProductResponseDto>>getProductsByCategory(
            @PathVariable Long id, Pageable pageable
    ){
       return ResponseEntity.ok(productService.getByCategory(id,pageable));
    }
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse>updateCategory(
            @PathVariable  Long id,
            @Valid @RequestBody CategoryRequest request
    ){
        CategoryResponse categoryResponse = categoryService.updateCategory(id,request);
        return ResponseEntity.ok(categoryResponse);
    }
}
