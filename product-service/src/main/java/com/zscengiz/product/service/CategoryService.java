package com.zscengiz.product.service;

import com.zscengiz.product.entity.Category;
import com.zscengiz.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;



     public Category add(Category category) {
         return categoryRepository.save(category);
     }
}
