package com.precisionpath.lab_service.dto;

import com.precisionpath.lab_service.entity.AppointmentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * What the queue-service needs to issue a token and estimate waiting time.
 */
public record InternalAppointmentResponse(
        UUID appointmentId,
        UUID patientId,
        String patientName,
        LocalDate appointmentDate,
        AppointmentStatus status,
        List<Test> tests,
        int totalDurationMinutes
) {

    public record Test(
            UUID labTestId,
            String code,
            String name,
            Integer durationMinutes
    ) {
    }
}
