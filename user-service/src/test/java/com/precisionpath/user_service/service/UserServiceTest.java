package com.precisionpath.user_service.service;

import com.precisionpath.user_service.dto.ChangePasswordRequest;
import com.precisionpath.user_service.dto.CreateStaffRequest;
import com.precisionpath.user_service.dto.UpdateProfileRequest;
import com.precisionpath.user_service.dto.UserProfileResponse;
import com.precisionpath.user_service.entity.Gender;
import com.precisionpath.user_service.entity.Role;
import com.precisionpath.user_service.entity.User;
import com.precisionpath.user_service.exception.InvalidCredentialsException;
import com.precisionpath.user_service.exception.ResourceNotFoundException;
import com.precisionpath.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User patient() {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName("Asha")
                .email("asha@example.com")
                .password("hashed")
                .phoneNumber("9876543210")
                .gender(Gender.FEMALE)
                .age(29)
                .role(Role.PATIENT)
                .build();
    }

    @Test
    void updateProfileChangesEditableFieldsOnly() {

        User user = patient();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse response = userService.updateProfile(
                user.getId(),
                new UpdateProfileRequest("Asha Rao", "9123456780", Gender.FEMALE, 30)
        );

        assertThat(response.fullName()).isEqualTo("Asha Rao");
        assertThat(response.phoneNumber()).isEqualTo("9123456780");
        assertThat(response.age()).isEqualTo(30);
        assertThat(response.email()).isEqualTo("asha@example.com");
    }

    @Test
    void getProfileThrowsWhenUserMissing() {

        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePasswordRequiresCurrentPassword() {

        User user = patient();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(
                user.getId(), new ChangePasswordRequest("wrong", "newPassword1")
        )).isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordStoresEncodedPassword() {

        User user = patient();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword1", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newPassword1")).thenReturn("new-hash");

        userService.changePassword(
                user.getId(), new ChangePasswordRequest("oldPassword1", "newPassword1")
        );

        assertThat(user.getPassword()).isEqualTo("new-hash");
        verify(userRepository).save(user);
    }

    @Test
    void createStaffRejectsPatientRole() {

        CreateStaffRequest request = new CreateStaffRequest(
                "Ravi", "ravi@example.com", "password123",
                "9876543210", Gender.MALE, 35, Role.PATIENT
        );

        assertThatThrownBy(() -> userService.createStaff(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteUserRejectsSelfDeletion() {

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> userService.deleteUser(id, id))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
