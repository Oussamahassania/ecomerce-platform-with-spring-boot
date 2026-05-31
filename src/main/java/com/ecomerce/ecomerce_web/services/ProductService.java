package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.ProductRequestDto;
import com.ecomerce.ecomerce_web.dtos.ProductResponseDto;
import com.ecomerce.ecomerce_web.entity.Product;
import com.ecomerce.ecomerce_web.mapper.ProductMapper;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ProductService {
    final private ProductRepository productRepository;
    private final ProductMapper productMapper;
    public ProductResponseDto createProduct(ProductRequestDto productDto){
        Product product = productMapper.toEntity(productDto);
        Product savedProduct = productRepository.save(product);
        return productMapper.toDto(savedProduct);
    }
    public ProductResponseDto updateProduct(ProductRequestDto productDto,Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product Not found"));
        productMapper.updateEntity(productDto,product);
        return productMapper.toDto(productRepository.save(product));
    }
    public List<ProductResponseDto> getAll() {
        return productRepository.findByActiveTrue()
                .stream()
                .map(productMapper::toDto)
                .toList();
    }
    public ProductResponseDto getById(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product Not found"));
        return productMapper.toDto(product);
    }
    public void delete(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setActive(false);
        productRepository.save(product);
    }


}
