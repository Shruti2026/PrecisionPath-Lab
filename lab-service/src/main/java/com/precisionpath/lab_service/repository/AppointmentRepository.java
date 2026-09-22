package com.precisionpath.lab_service.repository;

import com.precisionpath.lab_service.entity.Appointment;
import com.precisionpath.lab_service.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findAllByPatientIdOrderByAppointmentDateDescCreatedAtDesc(UUID patientId);

    Optional<Appointment> findByIdAndPatientId(UUID id, UUID patientId);

    boolean existsByPatientIdAndAppointmentDateAndStatusIn(
            UUID patientId,
            LocalDate appointmentDate,
            Collection<AppointmentStatus> statuses
    );

    @Query("""
            select a from Appointment a
            where (:date is null or a.appointmentDate = :date)
              and (:status is null or a.status = :status)
            order by a.appointmentDate asc, a.createdAt asc
            """)
    List<Appointment> search(
            @Param("date") LocalDate date,
            @Param("status") AppointmentStatus status
    );
}
