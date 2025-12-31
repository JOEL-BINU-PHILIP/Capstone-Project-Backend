package com.app.auth.repository;

import com.app.auth.model.TechnicianProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TechnicianProfileRepository extends MongoRepository<TechnicianProfile, String> {

    Optional<TechnicianProfile> findByUserId(String userId);

    // Find by approval status
    List<TechnicianProfile> findByApprovalStatus(TechnicianProfile.ApprovalStatus status);

    Page<TechnicianProfile> findByApprovalStatus(
            TechnicianProfile.ApprovalStatus status,
            Pageable pageable
    );

    // Find by assigned service/manager
    List<TechnicianProfile> findByAssignedServiceId(String serviceId);

    List<TechnicianProfile> findByAssignedManagerId(String managerId);

    // Find available technicians
    @Query("{'available': true, 'approvalStatus': 'APPROVED'}")
    List<TechnicianProfile> findAvailableTechnicians();

    // Find by city and skills
    @Query("{'city': ?0, 'skills': {$in: ?1}, 'approvalStatus': 'APPROVED', 'available': true}")
    List<TechnicianProfile> findByCityAndSkills(String city, List<String> skills);

    // Find top rated technicians
    @Query(value = "{'approvalStatus': 'APPROVED'}", sort = "{'averageRating': -1}")
    List<TechnicianProfile> findTopRatedTechnicians(Pageable pageable);

    // Count by status
    long countByApprovalStatus(TechnicianProfile.ApprovalStatus status);

    // Check if user has profile
    boolean existsByUserId(String userId);
}

