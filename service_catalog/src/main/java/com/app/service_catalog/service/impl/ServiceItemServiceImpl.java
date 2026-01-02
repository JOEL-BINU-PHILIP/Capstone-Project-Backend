package com.app.service_catalog. service.impl;

import com. app.service_catalog.dto. request.CreateServiceRequest;
import com.app.service_catalog.dto.request.UpdateServiceRequest;
import com.app.service_catalog.dto.response.PricingDetailsResponse;
import com.app.service_catalog.dto.response.ServiceItemResponse;
import com.app.service_catalog.exception.ResourceNotFoundException;
import com.app.service_catalog. model.ServiceCategory;
import com.app.service_catalog.model.ServiceItem;
import com. app.service_catalog.repository.ServiceCategoryRepository;
import com.app.service_catalog. repository.ServiceItemRepository;
import com.app.service_catalog. service.ServiceItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time. Instant;
import java.util. List;
import java.util. stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceItemRepository serviceItemRepository;
    private final ServiceCategoryRepository categoryRepository;

    // ==================== CREATE ====================

    @Override
    public ServiceItemResponse createService(CreateServiceRequest request) {
        // Validate category exists and get category name
        ServiceCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found:  " + request.getCategoryId()));

        ServiceItem serviceItem = ServiceItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .categoryId(request.getCategoryId())
                .categoryName(category.getName())
                .basePrice(request.getBasePrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes() != null ? request.getEstimatedDurationMinutes() : 60)
                .requiredSkills(request.getRequiredSkills())
                .imageUrl(request.getImageUrl())
                .taxPercentage(request.getTaxPercentage() != null ? request.getTaxPercentage() : 18.0)
                .discountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : 0.0)
                .discountValidUntil(request.getDiscountValidUntil())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ServiceItem saved = serviceItemRepository.save(serviceItem);
        log.info("Service created:  {}", saved.getId());

        // Update category service count
        category.setServicesCount(category.getServicesCount() + 1);
        categoryRepository.save(category);

        return mapToResponse(saved);
    }

    // ==================== UPDATE ====================

    @Override
    public ServiceItemResponse updateService(String serviceId, UpdateServiceRequest request) {
        ServiceItem serviceItem = getServiceEntity(serviceId);

        if (request.getName() != null) {
            serviceItem.setName(request.getName());
        }
        if (request.getDescription() != null) {
            serviceItem.setDescription(request.getDescription());
        }
        if (request.getBasePrice() != null) {
            serviceItem.setBasePrice(request.getBasePrice());
        }
        if (request.getCurrency() != null) {
            serviceItem.setCurrency(request.getCurrency());
        }
        if (request.getEstimatedDurationMinutes() != null) {
            serviceItem.setEstimatedDurationMinutes(request. getEstimatedDurationMinutes());
        }
        if (request.getRequiredSkills() != null) {
            serviceItem.setRequiredSkills(request.getRequiredSkills());
        }
        if (request. getImageUrl() != null) {
            serviceItem.setImageUrl(request.getImageUrl());
        }
        if (request. getTaxPercentage() != null) {
            serviceItem.setTaxPercentage(request. getTaxPercentage());
        }
        if (request.getDiscountPercentage() != null) {
            serviceItem.setDiscountPercentage(request. getDiscountPercentage());
        }
        if (request. getDiscountValidUntil() != null) {
            serviceItem.setDiscountValidUntil(request.getDiscountValidUntil());
        }

        serviceItem.setUpdatedAt(Instant.now());

        ServiceItem saved = serviceItemRepository.save(serviceItem);
        log.info("Service updated: {}", serviceId);

        return mapToResponse(saved);
    }

    @Override
    public ServiceItemResponse updateServiceStatus(String serviceId, boolean active) {
        ServiceItem serviceItem = getServiceEntity(serviceId);
        serviceItem.setActive(active);
        serviceItem.setUpdatedAt(Instant.now());

        ServiceItem saved = serviceItemRepository.save(serviceItem);
        log.info("Service {} status updated to: {}", serviceId, active);

        return mapToResponse(saved);
    }

    // ==================== DELETE ====================

    @Override
    public void deleteService(String serviceId) {
        ServiceItem serviceItem = getServiceEntity(serviceId);

        // Decrement category service count
        if (serviceItem.getCategoryId() != null) {
            categoryRepository.findById(serviceItem.getCategoryId())
                    .ifPresent(category -> {
                        category.setServicesCount(Math.max(0, category.getServicesCount() - 1));
                        categoryRepository.save(category);
                    });
        }

        serviceItemRepository.delete(serviceItem);
        log.info("Service deleted: {}", serviceId);
    }

    // ==================== GET SERVICES ====================

    @Override
    public List<ServiceItemResponse> getAllServices() {
        return serviceItemRepository.findAll().stream()
                .map(this:: mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceItemResponse> getActiveServices() {
        return serviceItemRepository.findByActive(true).stream()
                .map(this::mapToResponse)
                .collect(Collectors. toList());
    }

    @Override
    public ServiceItemResponse getServiceById(String serviceId) {
        ServiceItem serviceItem = getServiceEntity(serviceId);
        return mapToResponse(serviceItem);
    }

    @Override
    public List<ServiceItemResponse> getServicesByCategory(String categoryId) {
        return serviceItemRepository.findByCategoryId(categoryId).stream()
                .map(this:: mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceItemResponse> getServices(String categoryId, Boolean active) {
        List<ServiceItem> services;

        if (categoryId != null && active != null) {
            // Both filters
            services = serviceItemRepository.findByCategoryIdAndActive(categoryId, active);
        } else if (categoryId != null) {
            // Only category filter
            services = serviceItemRepository.findByCategoryId(categoryId);
        } else if (active != null) {
            // Only active filter
            services = serviceItemRepository.findByActive(active);
        } else {
            // No filters
            services = serviceItemRepository.findAll();
        }

        return services.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== SEARCH ====================

    @Override
    public List<ServiceItemResponse> search(String query, String skill) {
        List<ServiceItem> services;

        if (query != null && ! query.isEmpty() && skill != null && !skill.isEmpty()) {
            // Search by both query and skill
            List<ServiceItem> byName = serviceItemRepository.findByNameContainingIgnoreCase(query);
            List<ServiceItem> bySkill = serviceItemRepository. findByRequiredSkillsContainingIgnoreCase(skill);

            // Intersection of both results
            services = byName.stream()
                    .filter(bySkill::contains)
                    .collect(Collectors.toList());
        } else if (query != null && ! query.isEmpty()) {
            // Search by query only
            services = serviceItemRepository.findByNameContainingIgnoreCase(query);
        } else if (skill != null && ! skill.isEmpty()) {
            // Search by skill only
            services = serviceItemRepository.findByRequiredSkillsContainingIgnoreCase(skill);
        } else {
            // No search criteria - return active services
            services = serviceItemRepository.findByActive(true);
        }

        return services.stream()
                .map(this:: mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== HELPER METHODS ====================

    private ServiceItem getServiceEntity(String serviceId) {
        return serviceItemRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));
    }

    private ServiceItemResponse mapToResponse(ServiceItem serviceItem) {
        // Calculate pricing details
        PricingDetailsResponse pricingDetails = calculatePricing(serviceItem);

        return ServiceItemResponse.builder()
                .id(serviceItem.getId())
                .name(serviceItem. getName())
                .description(serviceItem.getDescription())
                .categoryId(serviceItem.getCategoryId())
                .categoryName(serviceItem.getCategoryName())
                .basePrice(serviceItem.getBasePrice())
                .currency(serviceItem.getCurrency())
                .estimatedDurationMinutes(serviceItem.getEstimatedDurationMinutes())
                .requiredSkills(serviceItem.getRequiredSkills())
                .imageUrl(serviceItem.getImageUrl())
                .active(serviceItem.isActive())
                .pricingDetails(pricingDetails)
                .createdAt(serviceItem.getCreatedAt())
                .updatedAt(serviceItem.getUpdatedAt())
                .build();
    }

    private PricingDetailsResponse calculatePricing(ServiceItem serviceItem) {
        double basePrice = serviceItem.getBasePrice();
        double taxPercentage = serviceItem.getTaxPercentage();
        double discountPercentage = serviceItem. getDiscountPercentage();

        double taxAmount = basePrice * (taxPercentage / 100);
        double discountAmount = basePrice * (discountPercentage / 100);
        double finalPrice = basePrice + taxAmount - discountAmount;

        return PricingDetailsResponse. builder()
                .basePrice(basePrice)
                .taxPercentage(taxPercentage)
                .taxAmount(Math.round(taxAmount * 100.0) / 100.0)
                .discountPercentage(discountPercentage)
                .discountAmount(Math.round(discountAmount * 100.0) / 100.0)
                .finalPrice(Math.round(finalPrice * 100.0) / 100.0)
                .discountValidUntil(serviceItem.getDiscountValidUntil())
                .build();
    }
}