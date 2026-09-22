package com.precisionpath.lab_service.client;

import java.util.UUID;

/**
 * Subset of the user-service's UserProfileResponse the lab-service needs.
 */
public record UserProfile(
        UUID userId,
        String fullName,
        String email,
        String phoneNumber,
        String gender,
        Integer age
) {
}
