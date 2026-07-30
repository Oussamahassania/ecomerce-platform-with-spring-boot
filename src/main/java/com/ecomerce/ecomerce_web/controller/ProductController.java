package com.ecomerce.ecomerce_web.controller;

import com.ecomerce.ecomerce_web.dtos.ProductRequestDto;
import com.ecomerce.ecomerce_web.dtos.ProductResponseDto;
import com.ecomerce.ecomerce_web.services.ProductService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@AllArgsConstructor
public class ProductController {
    final private ProductService productService;

    @PostMapping("/createProduct")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponseDto>createProduct(

            @RequestBody @Valid ProductRequestDto productDto
    ){
        ProductResponseDto response = productService.createProduct(productDto);
        return ResponseEntity.ok(response);
    }
    @PutMapping("/updateProduct/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponseDto>updateProduct(
            @PathVariable Long id
            ,@RequestBody @Valid
            ProductRequestDto productDto
    ){
        ProductResponseDto response =  productService.updateProduct(productDto,id);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/AllProducts")
    public ResponseEntity<List<ProductResponseDto>>displayAll(){
        List<ProductResponseDto>responses = productService.getAll();
        return ResponseEntity.ok(responses);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto>getById(
            @PathVariable Long id
    ){
        ProductResponseDto response = productService.getById(id);
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String>delete(
            @PathVariable Long id
    ){
        productService.delete(id);
        return ResponseEntity.ok("Product deleted successfully");
    }
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponseDto>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "ASC") String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size

    ){
        return ResponseEntity.ok(productService.searchProducts(
                name,null,null,null,sort,order,page,size
        ));
    }
    @GetMapping("/filter")
    public ResponseEntity<Page<ProductResponseDto>> filterProducts(
            @RequestParam(required = false)BigDecimal minPrice,
            @RequestParam(required = false)BigDecimal maxPrice,
            @RequestParam(required = false)Long categoryId,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "ASC") String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
     return ResponseEntity.ok(productService.searchProducts(
             null,minPrice,maxPrice,categoryId,sort,order,page,size
     ));
    }

}
