package com.precisionpath.lab_service.service;

import com.precisionpath.lab_service.dto.PaymentRequest;
import com.precisionpath.lab_service.dto.PaymentResponse;
import com.precisionpath.lab_service.entity.*;
import com.precisionpath.lab_service.exception.DuplicateResourceException;
import com.precisionpath.lab_service.exception.InvalidStateException;
import com.precisionpath.lab_service.exception.ResourceNotFoundException;
import com.precisionpath.lab_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Records payments. There is no payment gateway: online payments made by
 * patients are simulated and staff record counter payments (cash, card, UPI).
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final Clock clock;

    @Transactional
    public PaymentResponse pay(
            Appointment appointment,
            UUID payerId,
            PaymentRequest request,
            boolean recordedByStaff
    ) {

        if (!recordedByStaff && request.method() == PaymentMethod.CASH) {
            throw new IllegalArgumentException("Cash payments are accepted only at the lab counter");
        }

        if (appointment.getStatus() != AppointmentStatus.APPROVED) {
            throw new InvalidStateException("Payment can be made only after the appointment is approved");
        }

        if (paymentRepository.findByAppointmentId(appointment.getId()).isPresent()) {
            throw new DuplicateResourceException("This appointment has already been paid");
        }

        String reference = StringUtils.hasText(request.transactionReference())
                ? request.transactionReference().trim()
                : generateReference();

        if (paymentRepository.existsByTransactionReference(reference)) {
            throw new DuplicateResourceException("This transaction reference has already been used");
        }

        Payment payment = Payment.builder()
                .appointment(appointment)
                .amount(appointment.getTotalAmount())
                .method(request.method())
                .status(PaymentStatus.PAID)
                .transactionReference(reference)
                .recordedBy(payerId)
                .paidAt(LocalDateTime.now(clock))
                .build();

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID appointmentId) {

        return paymentRepository.findByAppointmentId(appointmentId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for this appointment"));
    }

    /** Called when a paid appointment is cancelled. */
    @Transactional
    public void refundIfPaid(Appointment appointment) {

        paymentRepository.findByAppointmentId(appointment.getId())
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.REFUNDED);
                    payment.setRefundedAt(LocalDateTime.now(clock));
                    paymentRepository.save(payment);
                });
    }

    private String generateReference() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase(Locale.ROOT);
    }
}
