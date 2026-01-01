package com.app.service_request.service;

import com.app.service_request.model.ServiceRequest;

import java.util.List;

public interface ServiceRequestService {

    ServiceRequest create(ServiceRequest request, String customerId);

    List<ServiceRequest> customerRequests(String customerId);

    ServiceRequest cancel(String requestId, String customerId);

    List<ServiceRequest> unassigned();

    ServiceRequest assign(String requestId, String technicianId, String managerId);

    List<ServiceRequest> technicianRequests(String technicianId);

    ServiceRequest start(String requestId, String technicianId);

    ServiceRequest complete(String requestId, String technicianId);
}
