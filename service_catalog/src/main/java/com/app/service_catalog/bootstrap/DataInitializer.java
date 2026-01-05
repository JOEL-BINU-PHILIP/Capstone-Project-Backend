package com.app.service_catalog.bootstrap;

import com.app.service_catalog.model.ServiceCategory;
import com.app.service_catalog.model.ServiceItem;
import com.app.service_catalog.repository.ServiceCategoryRepository;
import com.app.service_catalog.repository.ServiceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ServiceCategoryRepository categoryRepository;
    private final ServiceItemRepository serviceItemRepository;

    @Override
    public void run(String... args) {

        // 🛑 Prevent duplicate initialization
        if (categoryRepository.count() > 0) {
            log.info("Service catalog already initialized. Skipping.");
            return;
        }

        log.info("Initializing Service Catalog with real images...");

        List<ServiceCategory> categories = List.of(
                createCategory(
                        "AC Services",
                        "Air conditioner installation & repair",
                        "https://cdn-icons-png.flaticon.com/512/2933/2933786.png",
                        1
                ),
                createCategory(
                        "Plumbing",
                        "All plumbing related services",
                        "https://cdn-icons-png.flaticon.com/512/3105/3105807.png",
                        2
                ),
                createCategory(
                        "Electrical",
                        "Electrical repair & installation",
                        "https://cdn-icons-png.flaticon.com/512/1046/1046857.png",
                        3
                ),
                createCategory(
                        "Home Cleaning",
                        "Home & deep cleaning services",
                        "https://cdn-icons-png.flaticon.com/512/3043/3043888.png",
                        4
                ),
                createCategory(
                        "Appliance Repair",
                        "Repair household appliances",
                        "https://cdn-icons-png.flaticon.com/512/679/679720.png",
                        5
                ),
                createCategory(
                        "Carpentry",
                        "Woodwork & furniture services",
                        "https://cdn-icons-png.flaticon.com/512/3135/3135715.png",
                        6
                ),
                createCategory(
                        "Painting",
                        "Interior & exterior painting",
                        "https://cdn-icons-png.flaticon.com/512/3103/3103479.png",
                        7
                ),
                createCategory(
                        "Pest Control",
                        "Pest & termite control",
                        "https://cdn-icons-png.flaticon.com/512/616/616430.png",
                        8
                ),
                createCategory(
                        "Water Purifier",
                        "RO installation & service",
                        "https://cdn-icons-png.flaticon.com/512/2917/2917990.png",
                        9
                ),
                createCategory(
                        "Home Automation",
                        "Smart home installation",
                        "https://cdn-icons-png.flaticon.com/512/1705/1705736.png",
                        10
                )
        );

        categoryRepository.saveAll(categories);

        categories.forEach(category -> {
            serviceItemRepository.saveAll(List.of(
                    createService(category, "Installation", 999),
                    createService(category, "Maintenance", 1499),
                    createService(category, "Repair", 2499)
            ));

            category.setServicesCount(3);
            categoryRepository.save(category);
        });

        log.info("Service Catalog initialized successfully with images.");
    }

    private ServiceCategory createCategory(String name, String description, String iconUrl, int order) {
        return ServiceCategory.builder()
                .name(name)
                .description(description)
                .iconUrl(iconUrl)
                .displayOrder(order)
                .active(true)
                .servicesCount(0)
                .createdAt(Instant.now())
                .build();
    }

    private ServiceItem createService(ServiceCategory category, String suffix, double price) {
        return ServiceItem.builder()
                .name(category.getName() + " - " + suffix)
                .description(suffix + " for " + category.getName())
                .categoryId(category.getId())
                .categoryName(category.getName())
                .basePrice(price)
                .currency("INR")
                .estimatedDurationMinutes(60)
                .taxPercentage(18.0)
                .discountPercentage(0.0)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
