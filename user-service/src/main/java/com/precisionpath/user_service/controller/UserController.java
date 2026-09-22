package com.precisionpath.user_service.controller;

import com.precisionpath.user_service.dto.ChangePasswordRequest;
import com.precisionpath.user_service.dto.MessageResponse;
import com.precisionpath.user_service.dto.UpdateProfileRequest;
import com.precisionpath.user_service.dto.UserProfileResponse;
import com.precisionpath.user_service.security.AuthenticatedUser;
import com.precisionpath.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {

        return ResponseEntity.ok(
                userService.getProfile(currentUser.userId())
        );
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateProfileRequest request
    ) {

        return ResponseEntity.ok(
                userService.updateProfile(currentUser.userId(), request)
        );
    }

    @PutMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {

        userService.changePassword(currentUser.userId(), request);

        return ResponseEntity.ok(
                new MessageResponse("Password changed successfully")
        );
    }
}
