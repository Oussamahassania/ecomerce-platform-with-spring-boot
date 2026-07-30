package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.ProductRequestDto;
import com.ecomerce.ecomerce_web.dtos.ProductResponseDto;
import com.ecomerce.ecomerce_web.entity.Category;
import com.ecomerce.ecomerce_web.entity.Product;
import com.ecomerce.ecomerce_web.exception.InvalidRequestException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.mapper.ProductMapper;
import com.ecomerce.ecomerce_web.repository.CategoryRepository;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import com.ecomerce.ecomerce_web.specification.ProductSpecification;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name","price","createdAt","stock");


    @CacheEvict(value = "products", allEntries = true)
    public ProductResponseDto createProduct(ProductRequestDto productDto) {

        Product product = productMapper.toEntity(productDto);

        // Set the category from the categoryId
        resolveCategory(productDto.getCategoryId(), product);

        Product savedProduct = productRepository.save(product);

        return productMapper.toDto(savedProduct);
    }

    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "products", allEntries = true)
    })
    public ProductResponseDto updateProduct(ProductRequestDto productDto, Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        productMapper.updateEntity(productDto, product);

        // Update the category as well
        resolveCategory(productDto.getCategoryId(), product);

        Product updatedProduct = productRepository.save(product);

        return productMapper.toDto(updatedProduct);
    }

    @Cacheable(value = "products")
    public List<ProductResponseDto> getAll() {
        return productRepository.findByActiveTrue()
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Cacheable(value = "product", key = "#id")
    public ProductResponseDto getById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        return productMapper.toDto(product);
    }

    @CacheEvict(value = "products", allEntries = true)
    public void delete(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setActive(false);

        productRepository.save(product);
    }
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getByCategory(Long id, Pageable pageable) {

        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }

        return productRepository.findByCategoryIdAndActiveTrue(id, pageable)
                .map(productMapper::toDto);
    }

    private String validateSortField(String sort){
        if (sort == null || sort.isBlank()) return "createdAt";
        if (!ALLOWED_SORT_FIELDS.contains(sort)){
            throw new InvalidRequestException("Invalid sort field: " + sort);
        }
        return sort;
    }
    private int validatePageSize(int size){
        if (size < 1) return 10;
        return Math.min(size,100);
    }
    @Transactional(readOnly = true)
    public Page<ProductResponseDto>searchProducts(
            String name, BigDecimal minPrice,BigDecimal maxPrice,
            Long categoryId,String sort,String order,int page,int size){
        if (name != null && name.length() > 100) {
            throw new InvalidRequestException("Search term too long");
        }
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRequestException("minPrice cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidRequestException("minPrice cannot be greater than maxPrice");
        }
        String sortField = validateSortField(sort);
        int safeSize = validatePageSize(size);
        int safePage = Math.max(page,0);
        Sort.Direction direction = "desc" .equalsIgnoreCase(order) ?  Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(safePage,safeSize,Sort.by(direction,sortField));
        Specification<Product> spec = ProductSpecification.build(name,minPrice,maxPrice,categoryId);
        return productRepository.findAll(spec,pageable).map(productMapper::toDto);
    }


    private void resolveCategory(Long categoryId, Product product) {

        if (categoryId == null) {
            product.setCategory(null);
            return;
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id: " + categoryId));

        product.setCategory(category);
    }
}