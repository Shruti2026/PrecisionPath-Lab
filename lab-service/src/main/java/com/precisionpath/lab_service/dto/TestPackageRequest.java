package com.precisionpath.lab_service.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record TestPackageRequest(

        @NotBlank(message = "Package name is required")
        @Size(max = 150, message = "Package name must not exceed 150 characters")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Enter a valid price")
        BigDecimal price,

        @NotEmpty(message = "A package must contain at least 2 tests")
        @Size(min = 2, message = "A package must contain at least 2 tests")
        Set<@NotNull UUID> testIds
) {
}
