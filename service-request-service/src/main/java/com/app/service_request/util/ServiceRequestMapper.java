package com.app.service_request.util;

import com.app.service_request.dto.request.CreateServiceRequestDTO;
import com.app.service_request.dto.response.ServiceRequestResponseDTO;
import com.app.service_request.model.AddressSnapshot;
import com.app.service_request.model.ServiceRequest;

public class ServiceRequestMapper {

    public static ServiceRequest toEntity(CreateServiceRequestDTO dto) {
        ServiceRequest request = new ServiceRequest();
        request.setServiceId(dto.getServiceId());
        request.setProblemDescription(dto.getProblemDescription());
        request.setScheduledDate(dto.getScheduledDate());

        AddressSnapshot address = new AddressSnapshot();
        address.setAddressLine1(dto.getAddressLine1());
        address.setAddressLine2(dto.getAddressLine2());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setZipCode(dto.getZipCode());

        request.setAddressSnapshot(address);
        return request;
    }

    public static ServiceRequestResponseDTO toResponse(ServiceRequest entity) {
        ServiceRequestResponseDTO dto = new ServiceRequestResponseDTO();
        dto.setId(entity.getId());
        dto.setRequestNumber(entity.getRequestNumber());
        dto.setServiceId(entity.getServiceId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setAssignedTechnicianId(entity.getAssignedTechnicianId());
        dto.setStatus(entity.getStatus());
        dto.setRequestedAt(entity.getRequestedAt());
        dto.setAssignedAt(entity.getAssignedAt());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        return dto;
    }
}
