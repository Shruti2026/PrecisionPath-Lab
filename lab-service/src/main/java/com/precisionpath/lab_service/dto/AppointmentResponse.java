package com.precisionpath.lab_service.dto;

import com.precisionpath.lab_service.entity.AppointmentStatus;
import com.precisionpath.lab_service.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID patientId,
        String patientName,
        String patientPhone,
        LocalDate appointmentDate,
        AppointmentStatus status,
        List<AppointmentItemResponse> items,
        BigDecimal totalAmount,
        String reason,
        boolean prescriptionUploaded,
        String prescriptionFileName,
        String rejectionReason,
        /** Null until a payment is made. */
        PaymentStatus paymentStatus,
        Integer tokenNumber,
        Integer estimatedWaitMinutes,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
}
