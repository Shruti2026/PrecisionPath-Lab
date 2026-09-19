package com.precisionpath.user_service.dto;

import com.precisionpath.user_service.entity.Role;

import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        String fullName,
        String email,
        Role role,
        String message
) {
}
