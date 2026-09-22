package com.precisionpath.lab_service.dto;

import com.precisionpath.lab_service.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentRequest(

        @NotNull(message = "Payment method is required")
        PaymentMethod method,

        /** Optional; generated when not supplied (e.g. cash at the counter). */
        @Size(max = 100, message = "Transaction reference must not exceed 100 characters")
        String transactionReference
) {
}
