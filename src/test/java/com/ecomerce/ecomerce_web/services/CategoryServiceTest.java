package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.CategoryRequest;
import com.ecomerce.ecomerce_web.dtos.CategoryResponse;
import com.ecomerce.ecomerce_web.entity.Category;
import com.ecomerce.ecomerce_web.exception.DuplicateResourceException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.mapper.CategoryMapper;
import com.ecomerce.ecomerce_web.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryMapper categoryMapper;
    @Mock private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    private Category category;
    private CategoryRequest requestDto;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryMapper, categoryRepository);

        category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        requestDto = new CategoryRequest();
        requestDto.setName("Electronics");
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategory {

        @Test
        @DisplayName("should create when name is unique")
        void shouldCreate() {
            when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(false);
            when(categoryMapper.toEntity(requestDto)).thenReturn(category);
            when(categoryRepository.save(category)).thenReturn(category);
            CategoryResponse responseDto = new CategoryResponse();
            when(categoryMapper.toDto(category)).thenReturn(responseDto);

            CategoryResponse result = categoryService.createCategory(requestDto);

            assertThat(result).isEqualTo(responseDto);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when name already exists")
        void shouldRejectDuplicateName() {
            when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(true);

            assertThatThrownBy(() -> categoryService.createCategory(requestDto))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategory {

        @Test
        @DisplayName("should update when new name is unique")
        void shouldUpdate() {
            CategoryRequest updateDto = new CategoryRequest();
            updateDto.setName("Home Appliances");
            updateDto.setDescription("Updated desc");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByNameIgnoreCase("Home Appliances")).thenReturn(false);
            when(categoryRepository.save(category)).thenReturn(category);
            CategoryResponse responseDto = new CategoryResponse();
            when(categoryMapper.toDto(category)).thenReturn(responseDto);

            categoryService.updateCategory(1L, updateDto);

            assertThat(category.getName()).isEqualTo("Home Appliances");
        }

        @Test
        @DisplayName("should allow keeping the same name (case-insensitive) without duplicate error")
        void shouldAllowSameNameUnchanged() {
            CategoryRequest sameNameDto = new CategoryRequest();
            sameNameDto.setName("ELECTRONICS"); // same name, different case

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toDto(category)).thenReturn(new CategoryResponse());

            assertThatCode(() -> categoryService.updateCategory(1L, sameNameDto))
                    .doesNotThrowAnyException();

            verify(categoryRepository, never()).existsByNameIgnoreCase(any());
        }

        @Test
        @DisplayName("should throw when renaming to an already-taken name")
        void shouldRejectRenameToTakenName() {
            CategoryRequest updateDto = new CategoryRequest();
            updateDto.setName("Books");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByNameIgnoreCase("Books")).thenReturn(true);

            assertThatThrownBy(() -> categoryService.updateCategory(1L, updateDto))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should throw when category does not exist")
        void shouldThrowWhenCategoryMissing() {
            when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.updateCategory(404L, requestDto))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    @DisplayName("getAll should return all mapped categories")
    void shouldGetAll() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(categoryMapper.toDto(category)).thenReturn(new CategoryResponse());

        List<CategoryResponse> result = categoryService.getAll();

        assertThat(result).hasSize(1);
    }
}