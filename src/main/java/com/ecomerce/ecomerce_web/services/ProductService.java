package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.ProductRequestDto;
import com.ecomerce.ecomerce_web.dtos.ProductResponseDto;
import com.ecomerce.ecomerce_web.entity.Product;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ProductService {
    final private ProductRepository productRepository;
    public ProductResponseDto createProduct(ProductRequestDto productDto){
        Product product = toEntity(productDto);
        Product savedProduct = productRepository.save(product);
        return toDto(savedProduct);
    }
    public ProductResponseDto updateProduct(ProductRequestDto productDto,Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product Not found"));
        product.setName(productDto.getName());
        product.setPrice(productDto.getPrice());
        product.setDescription(productDto.getDescription());
        product.setImg_url(productDto.getImg_url());
        product.setStock(productDto.getStock());

        Product updatedProduct = productRepository.save(product);
        return toDto(updatedProduct);
    }
    public List<ProductResponseDto>getAll(){
        return productRepository
                .findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }
    public ProductResponseDto getById(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product Not found"));
        return toDto(product);
    }
    public void delete(Long id){
        productRepository.deleteById(id);
    }

    public Product toEntity(ProductRequestDto productDto){
        Product product =  new Product();
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setStock(productDto.getStock());
        product.setImg_url(productDto.getImg_url());
        return product;
    }
    public ProductResponseDto toDto(Product product){
        ProductResponseDto productResponseDto = new ProductResponseDto();
        productResponseDto.setId(product.getId());
        productResponseDto.setDescription(product.getDescription());
        productResponseDto.setName(product.getName());
        productResponseDto.setPrice(product.getPrice());
        productResponseDto.setStock(product.getStock());
        productResponseDto.setImg_url(product.getImg_url());
        productResponseDto.setCreatedAt(LocalDateTime.now());
        return  productResponseDto;
    }
}
