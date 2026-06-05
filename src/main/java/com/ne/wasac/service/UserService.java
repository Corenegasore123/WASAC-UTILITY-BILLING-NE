package com.ne.wasac.service;

import com.ne.wasac.dto.auth.CreateStaffRequest;
import com.ne.wasac.dto.auth.UserResponse;
import com.ne.wasac.enums.AuditAction;
import com.ne.wasac.enums.RoleName;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.enums.Status;
import com.ne.wasac.util.QuerySort;
import com.ne.wasac.exception.BusinessRuleException;
import com.ne.wasac.exception.ResourceNotFoundException;
import com.ne.wasac.model.AppUser;
import com.ne.wasac.model.Role;
import com.ne.wasac.repository.AppUserRepository;
import com.ne.wasac.repository.CustomerRepository;
import com.ne.wasac.repository.RoleRepository;
import com.ne.wasac.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$";

    private static final Map<String, Comparator<UserResponse>> SORT_FIELDS = Map.of(
            "id", Comparator.comparing(UserResponse::getId),
            "fullName", Comparator.comparing(UserResponse::getFullName, String.CASE_INSENSITIVE_ORDER),
            "email", Comparator.comparing(UserResponse::getEmail, String.CASE_INSENSITIVE_ORDER),
            "phoneNumber", Comparator.comparing(UserResponse::getPhoneNumber),
            "nationalId", Comparator.comparing(UserResponse::getNationalId),
            "status", Comparator.comparing(u -> u.getStatus().name()));

    private final AppUserRepository appUserRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional
    public UserResponse createStaff(CreateStaffRequest request) {
        if (request.getRole() == RoleName.ROLE_CUSTOMER || request.getRole() == RoleName.ROLE_ADMIN) {
            throw new BusinessRuleException("Only OPERATOR or FINANCE staff accounts can be created here");
        }
        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email already registered");
        }
        if (appUserRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessRuleException("Phone number already registered");
        }
        validateNationalIdAvailable(request.getNationalId());

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));

        String temporaryPassword = generateTemporaryPassword();

        AppUser user = new AppUser();
        user.setFullName(request.getFullName());
        user.setNationalId(request.getNationalId());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setStatus(Status.ACTIVE);
        user.setMustChangePassword(true);
        user.setRoles(new HashSet<>(Set.of(role)));

        AppUser saved = appUserRepository.save(user);
        emailService.sendStaffCredentials(saved.getEmail(), saved.getFullName(), saved.getEmail(),
                temporaryPassword, request.getRole());
        emailService.sendPasswordChangeReminder(saved.getEmail(), saved.getFullName());
        auditService.log(AuditAction.USER_CREATED, "AppUser", saved.getId(), null, request.getRole().name());

        return toUserResponse(saved);
    }

    @Transactional
    public UserResponse updateRole(Long userId, RoleName roleName) {
        if (roleName == RoleName.ROLE_ADMIN) {
            throw new BusinessRuleException("Cannot assign ADMIN role through this endpoint");
        }
        AppUser user = getUser(userId);
        String oldRole = user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.joining(","));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        user.setRoles(new HashSet<>(Set.of(role)));
        AppUser saved = appUserRepository.save(user);
        if (roleName == RoleName.ROLE_OPERATOR || roleName == RoleName.ROLE_FINANCE) {
            emailService.sendRoleUpgradeNotification(saved.getEmail(), saved.getFullName(), roleName);
        }
        auditService.log(AuditAction.USER_ROLE_CHANGED, "AppUser", saved.getId(), oldRole, roleName.name());

        return toUserResponse(saved);
    }

    @Transactional
    public UserResponse activate(Long userId) {
        AppUser user = getUser(userId);
        if (user.getStatus() == Status.ACTIVE) {
            throw new BusinessRuleException("User is already active");
        }
        Status old = user.getStatus();
        user.setStatus(Status.ACTIVE);
        user.setPendingEmailVerification(false);
        if (user.getCustomer() != null && user.getCustomer().getStatus() == Status.INACTIVE) {
            user.getCustomer().setStatus(Status.ACTIVE);
        }
        AppUser saved = appUserRepository.save(user);
        auditService.log(AuditAction.USER_ACTIVATED, "AppUser", saved.getId(), old.name(), Status.ACTIVE.name());
        return toUserResponse(saved);
    }

    @Transactional
    public UserResponse deactivate(Long userId) {
        Long currentId = SecurityUtils.currentUser().getUser().getId();
        if (userId.equals(currentId)) {
            throw new BusinessRuleException("You cannot deactivate your own account");
        }
        AppUser user = getUser(userId);
        if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN)) {
            long activeAdmins = appUserRepository.countByRoleAndStatus(RoleName.ROLE_ADMIN, Status.ACTIVE);
            if (activeAdmins <= 1 && user.getStatus() == Status.ACTIVE) {
                throw new BusinessRuleException("Cannot deactivate the only active administrator");
            }
        }
        if (user.getStatus() == Status.INACTIVE) {
            throw new BusinessRuleException("User is already inactive");
        }
        Status old = user.getStatus();
        user.setStatus(Status.INACTIVE);
        user.setPendingEmailVerification(false);
        AppUser saved = appUserRepository.save(user);
        auditService.log(AuditAction.USER_DEACTIVATED, "AppUser", saved.getId(), old.name(), Status.INACTIVE.name());
        return toUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll(String sortBy, SortDirection sortDir) {
        return sort(appUserRepository.findAll().stream().map(this::toUserResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> search(String q, Status status, RoleName role, String email,
                                     String sortBy, SortDirection sortDir) {
        return sort(appUserRepository.search(blankToNull(q), status, role, blankToNull(email))
                .stream().map(this::toUserResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> filterAndSort(String q, Status status, RoleName role, String email,
                                            String sortBy, SortDirection sortDir) {
        if (blankToNull(q) != null || status != null || role != null || blankToNull(email) != null) {
            return search(q, status, role, email, sortBy, sortDir);
        }
        return findAll(sortBy, sortDir);
    }

    private List<UserResponse> sort(List<UserResponse> items, String sortBy, SortDirection sortDir) {
        return QuerySort.apply(items, sortBy, sortDir == null ? SortDirection.ASC : sortDir,
                SORT_FIELDS, "fullName");
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toUserResponse(getUser(id));
    }

    private AppUser getUser(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private UserResponse toUserResponse(AppUser user) {
        Set<RoleName> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return new UserResponse(user.getId(), user.getFullName(), user.getNationalId(), user.getEmail(),
                user.getPhoneNumber(), user.getStatus(), user.isMustChangePassword(), roles);
    }

    private void validateNationalIdAvailable(String nationalId) {
        if (appUserRepository.existsByNationalId(nationalId)) {
            throw new BusinessRuleException("National ID already registered");
        }
        if (customerRepository.existsByNationalId(nationalId)) {
            throw new BusinessRuleException("National ID already exists");
        }
    }

    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    private String blankToNull(String value) {
        return QuerySort.blankToNull(value);
    }
}
