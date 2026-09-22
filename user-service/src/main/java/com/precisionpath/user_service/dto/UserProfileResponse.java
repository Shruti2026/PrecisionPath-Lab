package com.precisionpath.user_service.dto;

import com.precisionpath.user_service.entity.Gender;
import com.precisionpath.user_service.entity.Role;
import com.precisionpath.user_service.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileResponse(

        UUID userId,

        String fullName,

        String email,

        String phoneNumber,

        Gender gender,

        Integer age,

        Role role,

        LocalDateTime createdAt

) {

    public static UserProfileResponse from(User user) {

        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getGender(),
                user.getAge(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
