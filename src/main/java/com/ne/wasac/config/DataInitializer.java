package com.ne.wasac.config;

import com.ne.wasac.enums.RoleName;
import com.ne.wasac.enums.Status;
import com.ne.wasac.model.AppUser;
import com.ne.wasac.model.Role;
import com.ne.wasac.repository.AppUserRepository;
import com.ne.wasac.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Startup seed data: system roles + one admin user only.
 * No operator, finance, or customer accounts are seeded — those are created via registration or admin role assignment.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "corenegasore@gmail.com";
    private static final String ADMIN_NATIONAL_ID = "1199087766554401";
    private static final String ADMIN_PHONE = "0729023495";
    private static final String ADMIN_PASSWORD = "@Corene@WASAC@1";

    private final RoleRepository roleRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedRoles();
        seedAdminUser();
    }

    /** Creates all system roles if they do not already exist. */
    private void seedRoles() {
        Arrays.stream(RoleName.values()).forEach(roleName -> {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
                log.info("Created role {}", roleName);
            }
        });
    }

    /** Creates the default admin account when it does not already exist. */
    private void seedAdminUser() {
        appUserRepository.findByEmail(ADMIN_EMAIL).ifPresentOrElse(
                existing -> backfillAdminNationalId(existing),
                this::createAdminUser);
    }

    private void createAdminUser() {
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN must exist before seeding admin user"));

        AppUser admin = new AppUser();
        admin.setFullName("Corene Gasore");
        admin.setNationalId(ADMIN_NATIONAL_ID);
        admin.setEmail(ADMIN_EMAIL);
        admin.setPhoneNumber(ADMIN_PHONE);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setStatus(Status.ACTIVE);
        admin.setMustChangePassword(false);
        admin.setRoles(new HashSet<>(java.util.Set.of(adminRole)));

        appUserRepository.save(admin);
        log.info("Created default admin account {} (nationalId {})", ADMIN_EMAIL, ADMIN_NATIONAL_ID);
    }

    private void backfillAdminNationalId(AppUser admin) {
        if (admin.getNationalId() == null || admin.getNationalId().isBlank()) {
            admin.setNationalId(ADMIN_NATIONAL_ID);
            appUserRepository.save(admin);
            log.info("Set national ID on existing admin account {}", ADMIN_EMAIL);
        }
    }
}
