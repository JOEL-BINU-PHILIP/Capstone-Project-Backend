package com.app.service_catalog.repository;

import com.app.service_catalog.model.ServiceItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface ServiceItemRepository extends MongoRepository<ServiceItem, String>, PagingAndSortingRepository<ServiceItem, String> {

    List<ServiceItem> findByCategoryIdAndActiveTrue(String categoryId);
    List<ServiceItem> findByActiveTrue();
    List<ServiceItem> findByNameContainingIgnoreCase(String name);
    List<ServiceItem> findByRequiredSkillsContainingIgnoreCase(String skill);
    List<ServiceItem> findByCategoryId(String categoryId);
    List<ServiceItem> findByActive(boolean active);
    List<ServiceItem> findByCategoryIdAndActive(String categoryId, boolean active);

}
