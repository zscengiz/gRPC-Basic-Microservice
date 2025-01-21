package com.zscengiz.product.service;

import com.zscengiz.product.entity.Product;
import com.zscengiz.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    public Product add(Product product) {
        return repository.save(product);
    }
    public List<Product> getAll() {
        return repository.findAll();
    }

    public Product getById(int id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

    }
}
