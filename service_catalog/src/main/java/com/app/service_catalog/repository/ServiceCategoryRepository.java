package com.app.service_catalog.repository;

import com.app.service_catalog.model.ServiceCategory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ServiceCategoryRepository
        extends MongoRepository<ServiceCategory, String> {

    List<ServiceCategory> findByActiveTrueOrderByDisplayOrderAsc();

    // ✅ Used to block duplicates BEFORE saving
    boolean existsByNameIgnoreCase(String name);
}
