package com.ne.wasac.service;

import com.ne.wasac.dto.ProfileResponse;
import com.ne.wasac.dto.UpdateProfileRequest;
import com.ne.wasac.enums.AuditAction;
import com.ne.wasac.enums.RoleName;
import com.ne.wasac.exception.BusinessRuleException;
import com.ne.wasac.model.AppUser;
import com.ne.wasac.model.Customer;
import com.ne.wasac.model.Role;
import com.ne.wasac.repository.AppUserRepository;
import com.ne.wasac.security.SecurityUser;
import com.ne.wasac.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AppUserRepository appUserRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        return toProfile(SecurityUtils.currentUser().getUser());
    }

    @Transactional
    public ProfileResponse updateProfile(UpdateProfileRequest request) {
        SecurityUser current = SecurityUtils.currentUser();
        AppUser user = appUserRepository.findById(current.getUser().getId())
                .orElseThrow(() -> new BusinessRuleException("User not found"));

        if (!user.getPhoneNumber().equals(request.getPhoneNumber())
                && appUserRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessRuleException("Phone number already registered");
        }

        String oldName = user.getFullName();
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());

        if (user.getCustomer() != null) {
            Customer customer = user.getCustomer();
            customer.setFullName(request.getFullName());
            customer.setPhone(request.getPhoneNumber());
        }

        AppUser saved = appUserRepository.save(user);
        auditService.log(AuditAction.PROFILE_UPDATED, "AppUser", saved.getId(), oldName, saved.getFullName());
        return toProfile(saved);
    }

    private ProfileResponse toProfile(AppUser user) {
        Set<RoleName> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        Long customerId = null;
        String customerName = null;
        if (user.getCustomer() != null) {
            customerId = user.getCustomer().getId();
            customerName = user.getCustomer().getFullName();
        }
        return new ProfileResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhoneNumber(),
                user.getStatus(), user.isMustChangePassword(), roles, customerId, customerName);
    }
}
