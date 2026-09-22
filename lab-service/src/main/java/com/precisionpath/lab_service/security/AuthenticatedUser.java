package com.precisionpath.lab_service.security;

import java.util.UUID;

/**
 * Principal stored in the SecurityContext after a valid JWT is read.
 */
public record AuthenticatedUser(
        UUID userId,
        String email,
        String role
) {
}
