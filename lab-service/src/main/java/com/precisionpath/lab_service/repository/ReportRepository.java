package com.precisionpath.lab_service.repository;

import com.precisionpath.lab_service.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findAllByAppointmentIdOrderByUploadedAtAsc(UUID appointmentId);

    Optional<Report> findByIdAndAppointmentPatientId(UUID id, UUID patientId);
}
