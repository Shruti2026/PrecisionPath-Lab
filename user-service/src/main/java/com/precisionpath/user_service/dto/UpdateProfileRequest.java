package com.precisionpath.user_service.dto;

import com.precisionpath.user_service.entity.Gender;
import jakarta.validation.constraints.*;

public record UpdateProfileRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^[0-9]{10}$",
                message = "Phone number must contain exactly 10 digits"
        )
        String phoneNumber,

        @NotNull(message = "Gender is required")
        Gender gender,

        @NotNull(message = "Age is required")
        @Min(value = 1, message = "Age must be greater than 0")
        @Max(value = 120, message = "Enter a valid age")
        Integer age
) {
}
