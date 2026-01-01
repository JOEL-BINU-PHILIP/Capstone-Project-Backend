package com.app.service_request.repository;

import com.app.service_request.model.ServiceRequest;
import com.app.service_request.model.ServiceRequestStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRequestRepository extends MongoRepository<ServiceRequest, String> {

    // ======================
    // Customer
    // ======================
    List<ServiceRequest> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    // ======================
    // Service Manager
    // ======================
    List<ServiceRequest> findByStatus(ServiceRequestStatus status);

    // ======================
    // Technician
    // ======================
    List<ServiceRequest> findByAssignedTechnicianIdOrderByAssignedAtDesc(String technicianId);

    List<ServiceRequest> findByAssignedTechnicianIdAndStatus(
            String technicianId,
            ServiceRequestStatus status
    );

    // ======================
    // General
    // ======================
    Optional<ServiceRequest> findByRequestNumber(String requestNumber);
}
