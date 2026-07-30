package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.CategoryRequest;
import com.ecomerce.ecomerce_web.dtos.CategoryResponse;
import com.ecomerce.ecomerce_web.entity.Category;
import com.ecomerce.ecomerce_web.exception.DuplicateResourceException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.mapper.CategoryMapper;
import com.ecomerce.ecomerce_web.repository.CategoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CategoryService {
    private final CategoryMapper categoryMapper;
    private final CategoryRepository categoryRepository;

    public CategoryResponse createCategory(CategoryRequest dto){
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())){
            throw new DuplicateResourceException("Category is already exist: "+ dto.getName());
        }
        Category category = categoryMapper.toEntity(dto);
        return categoryMapper.toDto(categoryRepository.save(category));
    }
    public List<CategoryResponse>getAll(){
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toDto)
                .toList();
    }
    public Category getEntityById(Long id){
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category Not found with id: "+ id));
    }
    public CategoryResponse updateCategory(Long id, CategoryRequest dto){
        Category category = getEntityById(id);

        if (!category.getName().equalsIgnoreCase(dto.getName()) && categoryRepository.existsByNameIgnoreCase(dto.getName())){
            throw new DuplicateResourceException("Category is Already exist: "+ dto.getName());
        }
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        return categoryMapper.toDto(categoryRepository.save(category));

    }
}
