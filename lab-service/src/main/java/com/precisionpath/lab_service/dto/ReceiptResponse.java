package com.precisionpath.lab_service.dto;

import com.precisionpath.lab_service.entity.AppointmentStatus;
import com.precisionpath.lab_service.entity.PaymentMethod;
import com.precisionpath.lab_service.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReceiptResponse(
        String receiptNumber,
        LocalDateTime generatedAt,
        Patient patient,
        Appointment appointment,
        List<AppointmentItemResponse> items,
        List<TestDetail> tests,
        BigDecimal totalAmount,
        Payment payment,
        Queue queue
) {

    public record Patient(
            UUID patientId,
            String name,
            String email,
            String phone,
            Integer age,
            String gender
    ) {
    }

    public record Appointment(
            UUID appointmentId,
            LocalDate appointmentDate,
            AppointmentStatus status
    ) {
    }

    public record TestDetail(
            String code,
            String name,
            String purpose,
            String sampleType,
            List<String> prerequisites
    ) {
    }

    /** status is null when nothing has been paid yet. */
    public record Payment(
            PaymentStatus status,
            PaymentMethod method,
            String transactionReference,
            LocalDateTime paidAt
    ) {
    }

    /** Filled in by the queue-service; null values mean no token has been issued yet. */
    public record Queue(
            Integer tokenNumber,
            Integer estimatedWaitMinutes
    ) {
    }
}
