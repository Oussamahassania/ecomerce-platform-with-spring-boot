package com.ecomerce.ecomerce_web.controller;

import com.ecomerce.ecomerce_web.dtos.ProductRequestDto;
import com.ecomerce.ecomerce_web.dtos.ProductResponseDto;
import com.ecomerce.ecomerce_web.services.ProductService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/")
@AllArgsConstructor
public class ProductController {
    final private ProductService productService;

    @PostMapping("createProduct")
    public ResponseEntity<ProductResponseDto>createProduct(

            @RequestBody @Valid ProductRequestDto productDto
    ){
        ProductResponseDto response = productService.createProduct(productDto);
        return ResponseEntity.ok(response);
    }
    @PostMapping("updateProduct/{id}")
    public ResponseEntity<ProductResponseDto>updateProduct(
            @PathVariable Long id
            ,@RequestBody @Valid
            ProductRequestDto productDto
    ){
        ProductResponseDto response =  productService.updateProduct(productDto,id);
        return ResponseEntity.ok(response);
    }
    @GetMapping("AllProducts")
    public ResponseEntity<List<ProductResponseDto>>displayAll(){
        List<ProductResponseDto>responses = productService.getAll();
        return ResponseEntity.ok(responses);
    }
    @GetMapping("{id}")
    public ResponseEntity<ProductResponseDto>getById(
            @PathVariable Long id
    ){
        ProductResponseDto response = productService.getById(id);
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("delete/{id}")
    public ResponseEntity<String>delete(
            @PathVariable Long id
    ){
        productService.delete(id);
        return ResponseEntity.ok("Product deleted successfully");
    }

}
