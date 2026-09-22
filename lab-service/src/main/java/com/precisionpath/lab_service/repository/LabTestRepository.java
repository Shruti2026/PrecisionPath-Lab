package com.precisionpath.lab_service.repository;

import com.precisionpath.lab_service.entity.LabTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabTestRepository extends JpaRepository<LabTest, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    List<LabTest> findAllByActiveTrueOrderByNameAsc();

    List<LabTest> findAllByOrderByNameAsc();

    List<LabTest> findAllByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String name);

    Optional<LabTest> findByIdAndActiveTrue(UUID id);
}
