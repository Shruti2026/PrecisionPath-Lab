package com.precisionpath.lab_service.dto;

import com.precisionpath.lab_service.entity.Payment;
import com.precisionpath.lab_service.entity.PaymentMethod;
import com.precisionpath.lab_service.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID appointmentId,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        String transactionReference,
        LocalDateTime paidAt,
        LocalDateTime refundedAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAppointment().getId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getTransactionReference(),
                payment.getPaidAt(),
                payment.getRefundedAt()
        );
    }
}
