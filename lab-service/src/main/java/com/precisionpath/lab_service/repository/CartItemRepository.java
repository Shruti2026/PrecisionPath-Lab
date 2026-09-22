package com.precisionpath.lab_service.repository;

import com.precisionpath.lab_service.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findAllByPatientIdOrderByAddedAtAsc(UUID patientId);

    Optional<CartItem> findByIdAndPatientId(UUID id, UUID patientId);

    boolean existsByPatientIdAndLabTestId(UUID patientId, UUID labTestId);

    boolean existsByPatientIdAndTestPackageId(UUID patientId, UUID packageId);

    void deleteAllByPatientId(UUID patientId);
}
