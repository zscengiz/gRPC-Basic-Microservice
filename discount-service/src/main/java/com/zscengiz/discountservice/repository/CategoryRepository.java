package com.zscengiz.discountservice.repository;

import com.zscengiz.discountservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Optional<Category> findByExternalId(String externalId);


}
