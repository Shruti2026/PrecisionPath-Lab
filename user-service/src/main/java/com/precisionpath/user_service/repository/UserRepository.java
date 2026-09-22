package com.precisionpath.user_service.repository;

import com.precisionpath.user_service.entity.Role;
import com.precisionpath.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    List<User> findAllByRole(Role role);
}
