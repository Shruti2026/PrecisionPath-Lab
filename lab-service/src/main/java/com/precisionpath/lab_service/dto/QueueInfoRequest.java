package com.precisionpath.lab_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QueueInfoRequest(

        @NotNull(message = "Token number is required")
        @Min(value = 1, message = "Token number must be positive")
        Integer tokenNumber,

        @NotNull(message = "Estimated wait is required")
        @Min(value = 0, message = "Estimated wait must not be negative")
        Integer estimatedWaitMinutes
) {
}
