package com.app.service_catalog.service;

import com.app.service_catalog.dto.request.CreateServiceRequest;
import com.app.service_catalog.dto.request.ReorderCategoryRequest;
import com.app.service_catalog.dto.request.UpdateServiceRequest;
import com.app.service_catalog.dto.response.ServiceItemResponse;
import com.app.service_catalog.model.ServiceItem;
import org.springframework.data.domain.Page;

import java.awt.print.Pageable;
import java.util.List;

public interface ServiceItemService {

    ServiceItemResponse createService(CreateServiceRequest request);

    List<ServiceItemResponse> getAllServices();

    List<ServiceItemResponse> getServicesByCategory(String categoryId);

    ServiceItemResponse getServiceById(String serviceId);

    ServiceItemResponse updateService(String serviceId, UpdateServiceRequest request);

    ServiceItemResponse updateServiceStatus(String serviceId, boolean active);

    void deleteService(String serviceId);

    List<ServiceItemResponse> getActiveServices();

    List<ServiceItemResponse> getServices(String categoryId, Boolean active);

    List<ServiceItemResponse> search(String query, String skill);

//    Page<ServiceItemResponse> getPaged(Pageable pageable);

}
