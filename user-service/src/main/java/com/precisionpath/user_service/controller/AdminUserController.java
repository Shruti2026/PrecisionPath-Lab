package com.precisionpath.user_service.controller;

import com.precisionpath.user_service.dto.CreateStaffRequest;
import com.precisionpath.user_service.dto.MessageResponse;
import com.precisionpath.user_service.dto.UserProfileResponse;
import com.precisionpath.user_service.entity.Role;
import com.precisionpath.user_service.security.AuthenticatedUser;
import com.precisionpath.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getUsers(
            @RequestParam(required = false) Role role
    ) {

        return ResponseEntity.ok(userService.getUsers(role));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserProfileResponse> getUser(
            @PathVariable UUID userId
    ) {

        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PostMapping("/staff")
    public ResponseEntity<UserProfileResponse> createStaff(
            @Valid @RequestBody CreateStaffRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createStaff(request));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID userId
    ) {

        userService.deleteUser(userId, currentUser.userId());

        return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
    }
}
