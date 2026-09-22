package com.precisionpath.user_service.service;

import com.precisionpath.user_service.dto.ChangePasswordRequest;
import com.precisionpath.user_service.dto.CreateStaffRequest;
import com.precisionpath.user_service.dto.UpdateProfileRequest;
import com.precisionpath.user_service.dto.UserProfileResponse;
import com.precisionpath.user_service.entity.Role;
import com.precisionpath.user_service.entity.User;
import com.precisionpath.user_service.exception.DuplicateResourceException;
import com.precisionpath.user_service.exception.InvalidCredentialsException;
import com.precisionpath.user_service.exception.ResourceNotFoundException;
import com.precisionpath.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {

        return UserProfileResponse.from(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(
            UUID userId,
            UpdateProfileRequest request
    ) {

        User user = findUser(userId);

        user.setFullName(request.fullName().trim());
        user.setPhoneNumber(request.phoneNumber());
        user.setGender(request.gender());
        user.setAge(request.age());

        return UserProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(
            UUID userId,
            ChangePasswordRequest request
    ) {

        User user = findUser(userId);

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPassword()
        )) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new IllegalArgumentException(
                    "New password must be different from the current password"
            );
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getUsers(Role role) {

        List<User> users = role == null
                ? userRepository.findAll()
                : userRepository.findAllByRole(role);

        return users.stream()
                .map(UserProfileResponse::from)
                .toList();
    }

    @Transactional
    public UserProfileResponse createStaff(CreateStaffRequest request) {

        if (request.role() == Role.PATIENT) {
            throw new IllegalArgumentException(
                    "Patients must register through /api/auth/register"
            );
        }

        String email = AuthService.normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .phoneNumber(request.phoneNumber())
                .gender(request.gender())
                .age(request.age())
                .role(request.role())
                .build();

        return UserProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID userId, UUID requesterId) {

        if (userId.equals(requesterId)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }

        userRepository.delete(findUser(userId));
    }

    private User findUser(UUID userId) {

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );
    }
}
