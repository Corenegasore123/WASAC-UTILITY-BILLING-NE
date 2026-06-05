package com.ne.wasac.repository;

import com.ne.wasac.enums.RoleName;
import com.ne.wasac.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
