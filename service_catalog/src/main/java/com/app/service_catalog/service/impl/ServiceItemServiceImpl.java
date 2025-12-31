package com.app.service_catalog.service.impl;

import com.app.service_catalog.dto.request.CreateServiceRequest;
import com.app.service_catalog.dto.request.UpdateServiceRequest;
import com.app.service_catalog.dto.response.PricingDetailsResponse;
import com.app.service_catalog.dto.response.ServiceItemResponse;
import com.app.service_catalog.model.ServiceCategory;
import com.app.service_catalog.model.ServiceItem;
import com.app.service_catalog.repository.ServiceCategoryRepository;
import com.app.service_catalog.repository.ServiceItemRepository;
import com.app.service_catalog.service.ServiceItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.awt.print.Pageable;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceItemRepository serviceRepository;
    private final ServiceCategoryRepository categoryRepository;

    @Override
    public ServiceItemResponse createService(CreateServiceRequest request) {

        ServiceCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        ServiceItem service = ServiceItem.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .name(request.getName())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .currency(request.getCurrency() == null ? "USD" : request.getCurrency())
                .estimatedDurationMinutes(
                        request.getEstimatedDurationMinutes() == null ? 0 : request.getEstimatedDurationMinutes()
                )
                .imageUrl(request.getImageUrl())
                .requiredSkills(request.getRequiredSkills())
                .taxPercentage(request.getTaxPercentage() == null ? 0 : request.getTaxPercentage())
                .discountPercentage(request.getDiscountPercentage() == null ? 0 : request.getDiscountPercentage())
                .discountValidUntil(request.getDiscountValidUntil())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ServiceItem saved = serviceRepository.save(service);

        // increment category service count
        category.setServicesCount(category.getServicesCount() + 1);
        categoryRepository.save(category);

        return mapToResponse(saved);
    }

    @Override
    public List<ServiceItemResponse> getAllServices() {
        return serviceRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ServiceItemResponse> getServicesByCategory(String categoryId) {
        return serviceRepository.findByCategoryIdAndActiveTrue(categoryId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public ServiceItemResponse getServiceById(String serviceId) {
        ServiceItem service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        return mapToResponse(service);
    }

    // -----------------------------
    // MAPPER
    // -----------------------------
    private ServiceItemResponse mapToResponse(ServiceItem service) {

        double taxAmount = service.getBasePrice() * service.getTaxPercentage() / 100;
        double discountAmount = service.getBasePrice() * service.getDiscountPercentage() / 100;
        double finalPrice = service.getBasePrice() + taxAmount - discountAmount;

        PricingDetailsResponse pricing = PricingDetailsResponse.builder()
                .basePrice(service.getBasePrice())
                .taxPercentage(service.getTaxPercentage())
                .taxAmount(taxAmount)
                .discountPercentage(service.getDiscountPercentage())
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .discountValidUntil(service.getDiscountValidUntil())
                .build();

        return ServiceItemResponse.builder()
                .id(service.getId())
                .categoryId(service.getCategoryId())
                .categoryName(service.getCategoryName())
                .name(service.getName())
                .description(service.getDescription())
                .basePrice(service.getBasePrice())
                .currency(service.getCurrency())
                .estimatedDurationMinutes(service.getEstimatedDurationMinutes())
                .imageUrl(service.getImageUrl())
                .active(service.isActive())
                .requiredSkills(service.getRequiredSkills())
                .pricingDetails(pricing)
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }

    @Override
    public ServiceItemResponse updateService(String serviceId, UpdateServiceRequest request) {

        ServiceItem service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        if (request.getName() != null)
            service.setName(request.getName());

        if (request.getDescription() != null)
            service.setDescription(request.getDescription());

        if (request.getBasePrice() != null)
            service.setBasePrice(request.getBasePrice());

        if (request.getCurrency() != null)
            service.setCurrency(request.getCurrency());

        if (request.getEstimatedDurationMinutes() != null)
            service.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());

        if (request.getImageUrl() != null)
            service.setImageUrl(request.getImageUrl());

        if (request.getRequiredSkills() != null)
            service.setRequiredSkills(request.getRequiredSkills());

        if (request.getTaxPercentage() != null)
            service.setTaxPercentage(request.getTaxPercentage());

        if (request.getDiscountPercentage() != null)
            service.setDiscountPercentage(request.getDiscountPercentage());

        if (request.getDiscountValidUntil() != null)
            service.setDiscountValidUntil(request.getDiscountValidUntil());

        service.setUpdatedAt(Instant.now());

        return mapToResponse(serviceRepository.save(service));
    }

    public ServiceItemResponse updateServiceStatus(String serviceId, boolean active) {

        ServiceItem service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        service.setActive(active);
        service.setUpdatedAt(Instant.now());

        return mapToResponse(serviceRepository.save(service));
    }

    public void deleteService(String serviceId) {

        ServiceItem service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        ServiceCategory category = categoryRepository.findById(service.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setServicesCount(Math.max(0, category.getServicesCount() - 1));
        categoryRepository.save(category);

        serviceRepository.delete(service);
    }

    @Override
    public List<ServiceItemResponse> getActiveServices() {
        return serviceRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ServiceItemResponse> getServices(String categoryId, Boolean active) {

        List<ServiceItem> services;

        if (categoryId != null && active != null) {
            services = serviceRepository.findByCategoryIdAndActive(categoryId, active);
        } else if (categoryId != null) {
            services = serviceRepository.findByCategoryId(categoryId);
        } else if (active != null) {
            services = serviceRepository.findByActive(active);
        } else {
            services = serviceRepository.findAll();
        }

        return services.stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<ServiceItemResponse> search(String query, String skill) {

        if (query != null) {
            return serviceRepository.findByNameContainingIgnoreCase(query)
                    .stream().map(this::mapToResponse).toList();
        }

        if (skill != null) {
            return serviceRepository.findByRequiredSkillsContainingIgnoreCase(skill)
                    .stream().map(this::mapToResponse).toList();
        }

        return List.of();
    }

//    @Override
//    public Page<ServiceItemResponse> getPaged(Pageable pageable) {
//        return serviceRepository.findAll(pageable)
//                .map(this::mapToResponse);
//    }


}
