package com.precisionpath.lab_service.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record LabTestRequest(

        @NotBlank(message = "Test code is required")
        @Size(max = 30, message = "Test code must not exceed 30 characters")
        String code,

        @NotBlank(message = "Test name is required")
        @Size(max = 150, message = "Test name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Purpose is required")
        String purpose,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Enter a valid price")
        BigDecimal price,

        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        @Max(value = 480, message = "Duration must not exceed 480 minutes")
        Integer durationMinutes,

        @Size(max = 100, message = "Sample type must not exceed 100 characters")
        String sampleType,

        @Min(value = 1, message = "Report turnaround must be at least 1 hour")
        Integer reportTurnaroundHours,

        List<@NotBlank(message = "Prerequisite must not be blank")
             @Size(max = 500, message = "Prerequisite must not exceed 500 characters") String> prerequisites
) {
}
