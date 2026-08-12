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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
     @Mock
     private ProductRepository productRepository;
     @Mock
     private CategoryRepository categoryRepository;
     @Mock
     private ProductMapper productMapper;
     @InjectMocks
     private ProductService productService;
     private Product product;
     private ProductRequestDto requestDto;
     private ProductResponseDto responseDto;
     private Category category;

     @BeforeEach
     void setUp(){
         category = new Category();
         category.setId(1L);
         category.setName("Electronics");

         product = new Product();
         product.setId(1L);
         product.setName("iPhone 15 Pro");
         product.setPrice(BigDecimal.valueOf(999.99));
         product.setStock(50);
         product.setActive(true);
         product.setCategory(category);

         responseDto = new ProductResponseDto();
         responseDto.setId(1L);
         responseDto.setName("iPhone 15 Pro");
         responseDto.setPrice(BigDecimal.valueOf(999.99));
         responseDto.setStock(50);
         responseDto.setCategoryId(1L);
         responseDto.setCategoryName("Electronics");

         requestDto = new ProductRequestDto();
         requestDto.setName("iPhone 15 Pro");
         requestDto.setDescription("A great phone with a long enough description");
         requestDto.setPrice(BigDecimal.valueOf(999.99));
         requestDto.setStock(50);
         requestDto.setCategoryId(1L);
     }

     @Nested
     @DisplayName("createProduct")
     class CreateProduct{

         @Test
         @DisplayName("should create product with a  valid category")
         void shouldCreateProductWithValidCategory(){
             when(productMapper.toEntity(requestDto)).thenReturn(product);
             when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
             when(productRepository.save(product)).thenReturn(product);
             when(productMapper.toDto(product)).thenReturn(responseDto);


             ProductResponseDto result = productService.createProduct(requestDto);
             assertThat(result).isEqualTo(responseDto);
             assertThat(product.getCategory()).isEqualTo(category);
             verify(productRepository).save(product);
         }
         @Test
         @DisplayName("should throw when categoryId does not exist")
         void shouldThrowWhenCategoryNotFound(){
             when(productMapper.toEntity(requestDto)).thenReturn(product);
             when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

             assertThatThrownBy(() -> productService.createProduct(requestDto))
                     .isInstanceOf(ResourceNotFoundException.class)
                     .hasMessageContaining("Category not found with id: " + category.getId());

             verify(productRepository,never()).save(any());
         }

         @Test
         @DisplayName("should allow null categoryId (uncategorized product)")
         void shouldAllowNullCategory(){
             requestDto.setCategoryId(null);
             when(productMapper.toEntity(requestDto)).thenReturn(product);
             when(productRepository.save(product)).thenReturn(product);
             when(productMapper.toDto(product)).thenReturn(responseDto);

             productService.createProduct(requestDto);
             assertThat(product.getCategory()).isNull();
             verify(categoryRepository,never()).findById(any());
         }
     }
    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct{
        @Test
        @DisplayName("should update existing product")
        void shouldUpdateProduct(){
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(productRepository.save(product)).thenReturn(product);
            when(productMapper.toDto(product)).thenReturn(responseDto);

            ProductResponseDto result = productService.updateProduct(requestDto, 1L);

            assertThat(result).isEqualTo(responseDto);
            verify(productMapper).updateEntity(requestDto, product);
        }
        @Test
        @DisplayName("should throw when product does not exist")
        void shouldThrowWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(requestDto, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product not found");
        }

    }
    @Nested
    @DisplayName("getAll and getById")
    class ReadOperations{

        @Test
        @DisplayName("getAll should return only active products")
        void shouldReturnActiveProducts() {
            when(productRepository.findByActiveTrue()).thenReturn(List.of(product));
            when(productMapper.toDto(product)).thenReturn(responseDto);

            List<ProductResponseDto> result = productService.getAll();

            assertThat(result).hasSize(1).containsExactly(responseDto);
        }
        @Test
        @DisplayName("getById should return product when found")
        void shouldReturnProductById() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productMapper.toDto(product)).thenReturn(responseDto);

            ProductResponseDto result = productService.getById(1L);

            assertThat(result).isEqualTo(responseDto);
        }
        @Test
        @DisplayName("getById should throw ResourceNotFoundException when missing")
        void shouldThrowWhenNotFoundById() {
            when(productRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getById(404L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
    @Nested
    @DisplayName("delete")
    class Delete{
        @Test
        @DisplayName("should soft-delete by setting active=false, not remove row")
        void shouldSoftDeleteProduct() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            productService.delete(1L);

            assertThat(product.isActive()).isFalse();
            verify(productRepository).save(product);
            verify(productRepository, never()).delete(any(Product.class));
            verify(productRepository, never()).deleteById(any());
        }
        @Test
        @DisplayName("should throw when deleting nonexistent product")
        void shouldThrowWhenDeletingMissingProduct() {
            when(productRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.delete(404L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
    @Nested
    @DisplayName("searchProducts - validation")
    class SearchValidation {

        @Test
        @DisplayName("should throw when name exceeds 100 characters")
        void shouldRejectOverlongName() {
            String longName = "a".repeat(101);

            assertThatThrownBy(() -> productService.searchProducts(
                    longName, null, null, null, null, "asc", 0, 10))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("too long");

            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("should throw when minPrice is negative")
        void shouldRejectNegativeMinPrice() {
            assertThatThrownBy(() -> productService.searchProducts(
                    null, BigDecimal.valueOf(-1), null, null, null, "asc", 0, 10))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("cannot be negative");
        }

        @Test
        @DisplayName("should throw when minPrice > maxPrice")
        void shouldRejectInvertedPriceRange() {
            assertThatThrownBy(() -> productService.searchProducts(
                    null, BigDecimal.valueOf(500), BigDecimal.valueOf(10),
                    null, null, "asc", 0, 10))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("greater than maxPrice");
        }

        @Test
        @DisplayName("should throw on invalid sort field")
        void shouldRejectInvalidSortField() {

            assertThatThrownBy(() -> productService.searchProducts(
                    null, null, null, null,
                    "orderItems",
                    "asc",
                    0,
                    10))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Invalid sort field");

            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("should cap page size at 100 even if larger is requested")
        void shouldCapPageSize() {
            when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(product)));
            when(productMapper.toDto(product)).thenReturn(responseDto);

            productService.searchProducts(null, null, null, null, null, "asc", 0, 99999);

            var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
            verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("should default sort field to createdAt when not provided")
        void shouldDefaultSortField() {
            when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(product)));
            when(productMapper.toDto(product)).thenReturn(responseDto);

            productService.searchProducts(null, null, null, null, null, "asc", 0, 10);

            var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
            verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        }
    }
    @Nested
    @DisplayName("searchProducts - success")
    class SearchSuccess {

        @Test
        @DisplayName("should return mapped page of results")
        void shouldReturnMappedResults() {
            Page<Product> productPage = new PageImpl<>(List.of(product));
            when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(productPage);
            when(productMapper.toDto(product)).thenReturn(responseDto);

            Page<ProductResponseDto> result = productService.searchProducts(
                    "iPhone", null, null, null, "price", "desc", 0, 10);

            assertThat(result.getContent()).containsExactly(responseDto);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getByCategory")
    class GetByCategory {

        @Test
        @DisplayName("should throw when category does not exist")
        void shouldThrowWhenCategoryMissing() {
            when(categoryRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> productService.getByCategory(99L, PageRequest.of(0, 10)))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(productRepository, never()).findByCategoryIdAndActiveTrue(any(), any());
        }

        @Test
        @DisplayName("should return products for existing category")
        void shouldReturnProductsForCategory() {
            when(categoryRepository.existsById(1L)).thenReturn(true);
            when(productRepository.findByCategoryIdAndActiveTrue(eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(product)));
            when(productMapper.toDto(product)).thenReturn(responseDto);

            Page<ProductResponseDto> result = productService.getByCategory(1L, PageRequest.of(0, 10));

            assertThat(result.getContent()).containsExactly(responseDto);
        }
    }
    @Nested
    @DisplayName("updateStock")
    class UpdateStock {

        @Test
        @DisplayName("should update stock to new value")
        void shouldUpdateStock() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);
            when(productMapper.toDto(product)).thenReturn(responseDto);

            productService.updateStock(1L, 5);

            assertThat(product.getStock()).isEqualTo(5);
        }

        @Test
        @DisplayName("should throw when product not found")
        void shouldThrowWhenUpdatingStockOnMissingProduct() {
            when(productRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateStock(404L, 5))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }


    @Nested
    @DisplayName("getLowStockProducts")
    class LowStock {

        @Test
        @DisplayName("should return products below threshold")
        void shouldReturnLowStockProducts() {
            when(productRepository.findByStockLessThanAndActiveTrue(10))
                    .thenReturn(List.of(product));
            when(productMapper.toDto(product)).thenReturn(responseDto);

            List<ProductResponseDto> result = productService.getLowStockProducts();

            assertThat(result).containsExactly(responseDto);
        }

        @Test
        @DisplayName("should return empty list when nothing is low on stock")
        void shouldReturnEmptyWhenNoneLow() {
            when(productRepository.findByStockLessThanAndActiveTrue(10))
                    .thenReturn(List.of());

            List<ProductResponseDto> result = productService.getLowStockProducts();

            assertThat(result).isEmpty();
            verifyNoInteractions(productMapper);
        }
    }
}
