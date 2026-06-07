package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.ProductRequestDto;
import com.ecomerce.ecomerce_web.dtos.ProductResponseDto;
import com.ecomerce.ecomerce_web.entity.Product;
import com.ecomerce.ecomerce_web.mapper.ProductMapper;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ProductService {
    final private ProductRepository productRepository;
    private final ProductMapper productMapper;
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponseDto createProduct(ProductRequestDto productDto){
        Product product = productMapper.toEntity(productDto);
        Product savedProduct = productRepository.save(product);
        return productMapper.toDto(savedProduct);
    }
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "products", allEntries = true)
    })
    public ProductResponseDto updateProduct(ProductRequestDto productDto,Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product Not found"));
        productMapper.updateEntity(productDto,product);
        return productMapper.toDto(productRepository.save(product));
    }
    @Cacheable(value = "products")
    public List<ProductResponseDto> getAll() {
        return productRepository.findByActiveTrue()
                .stream()
                .map(productMapper::toDto)
                .toList();
    }
    @Cacheable(value = "product",key = "#id")
    public ProductResponseDto getById(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product Not found"));
        return productMapper.toDto(product);
    }
    @CacheEvict(value = "products", allEntries = true)
    public void delete(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setActive(false);
        productRepository.save(product);
    }


}
