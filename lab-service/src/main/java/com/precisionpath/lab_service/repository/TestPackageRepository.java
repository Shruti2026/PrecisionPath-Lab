package com.precisionpath.lab_service.repository;

import com.precisionpath.lab_service.entity.TestPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestPackageRepository extends JpaRepository<TestPackage, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    List<TestPackage> findAllByActiveTrueOrderByNameAsc();

    List<TestPackage> findAllByOrderByNameAsc();

    Optional<TestPackage> findByIdAndActiveTrue(UUID id);
}
