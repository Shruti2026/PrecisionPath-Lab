package com.precisionpath.user_service.config;

import com.precisionpath.user_service.entity.Gender;
import com.precisionpath.user_service.entity.Role;
import com.precisionpath.user_service.entity.User;
import com.precisionpath.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Creates the first ADMIN account on startup when none exists,
 * using ADMIN_EMAIL / ADMIN_PASSWORD. Further staff are created
 * by that admin through /api/admin/staff.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {

        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            log.warn("No ADMIN account exists. Set ADMIN_EMAIL and ADMIN_PASSWORD to create one on startup.");
            return;
        }

        if (userRepository.existsByEmail(adminEmail.toLowerCase().trim())) {
            log.warn("ADMIN_EMAIL {} is already used by a non-admin account; skipping admin creation.", adminEmail);
            return;
        }

        User admin = User.builder()
                .fullName("System Admin")
                .email(adminEmail.toLowerCase().trim())
                .password(passwordEncoder.encode(adminPassword))
                .phoneNumber("0000000000")
                .gender(Gender.OTHER)
                .age(30)
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);

        log.info("Created initial ADMIN account {}", admin.getEmail());
    }
}
