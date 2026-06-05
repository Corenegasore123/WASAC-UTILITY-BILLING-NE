package com.ne.wasac.service;

import com.ne.wasac.dto.CreateCustomerRequest;
import com.ne.wasac.dto.CreateCustomerResponse;
import com.ne.wasac.dto.CustomerRequest;
import com.ne.wasac.dto.CustomerResponse;
import com.ne.wasac.enums.AuditAction;
import com.ne.wasac.enums.BillStatus;
import com.ne.wasac.enums.MeterStatus;
import com.ne.wasac.enums.RoleName;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.enums.Status;
import com.ne.wasac.util.QuerySort;
import com.ne.wasac.exception.BusinessRuleException;
import com.ne.wasac.exception.ResourceNotFoundException;
import com.ne.wasac.model.AppUser;
import com.ne.wasac.model.Customer;
import com.ne.wasac.model.Role;
import com.ne.wasac.repository.AppUserRepository;
import com.ne.wasac.repository.BillRepository;
import com.ne.wasac.repository.CustomerRepository;
import com.ne.wasac.repository.MeterRepository;
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

/**
 * Customer profile CRUD with duplicate prevention and delete guards.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$";

    private static final Map<String, Comparator<CustomerResponse>> SORT_FIELDS = Map.of(
            "id", Comparator.comparing(CustomerResponse::getId),
            "fullName", Comparator.comparing(CustomerResponse::getFullName, String.CASE_INSENSITIVE_ORDER),
            "email", Comparator.comparing(CustomerResponse::getEmail, String.CASE_INSENSITIVE_ORDER),
            "phone", Comparator.comparing(CustomerResponse::getPhone),
            "nationalId", Comparator.comparing(CustomerResponse::getNationalId),
            "status", Comparator.comparing(c -> c.getStatus().name()));

    private final CustomerRepository customerRepository;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final MeterRepository meterRepository;
    private final BillRepository billRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    /**
     * Admin creates customer profile + login. Account is ACTIVE immediately;
     * temporary password is emailed and must be changed on first login.
     */
    @Transactional
    public CreateCustomerResponse createByAdmin(CreateCustomerRequest request) {
        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email already registered");
        }
        if (appUserRepository.existsByPhoneNumber(request.getPhone())) {
            throw new BusinessRuleException("Phone number already registered");
        }
        validateNationalIdAvailable(request.getNationalId());

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer role not found"));

        String temporaryPassword = generateTemporaryPassword();

        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setNationalId(request.getNationalId());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setStatus(Status.ACTIVE);
        Customer savedCustomer = customerRepository.save(customer);

        AppUser user = new AppUser();
        user.setFullName(request.getFullName());
        user.setNationalId(request.getNationalId());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhone());
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setStatus(Status.ACTIVE);
        user.setMustChangePassword(true);
        user.setRoles(new HashSet<>(Set.of(customerRole)));
        user.setCustomer(savedCustomer);
        AppUser savedUser = appUserRepository.save(user);

        emailService.sendCustomerCredentials(savedUser.getEmail(), savedUser.getFullName(),
                savedUser.getEmail(), temporaryPassword);
        emailService.sendPasswordChangeReminder(savedUser.getEmail(), savedUser.getFullName());
        auditService.log(AuditAction.USER_CREATED, "AppUser", savedUser.getId(), null, RoleName.ROLE_CUSTOMER.name());

        return new CreateCustomerResponse(
                "Customer account created. Temporary password sent to email.",
                DtoMapper.toCustomerResponse(savedCustomer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll(String sortBy, SortDirection sortDir) {
        return sort(customerRepository.findAll().stream().map(DtoMapper::toCustomerResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> search(String q, Status status, String email,
                                         String sortBy, SortDirection sortDir) {
        return sort(customerRepository.search(blankToNull(q), status, blankToNull(email))
                .stream().map(DtoMapper::toCustomerResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> filterAndSort(String q, Status status, String email,
                                                String sortBy, SortDirection sortDir) {
        if (blankToNull(q) != null || status != null || blankToNull(email) != null) {
            return search(q, status, email, sortBy, sortDir);
        }
        return findAll(sortBy, sortDir);
    }

    private List<CustomerResponse> sort(List<CustomerResponse> items, String sortBy, SortDirection sortDir) {
        return QuerySort.apply(items, sortBy, sortDir == null ? SortDirection.ASC : sortDir,
                SORT_FIELDS, "fullName");
    }

    @Transactional
    public CustomerResponse activate(Long id) {
        Customer customer = getCustomer(id);
        if (customer.getStatus() == Status.ACTIVE) {
            throw new BusinessRuleException("Customer is already active");
        }
        Status old = customer.getStatus();
        customer.setStatus(Status.ACTIVE);
        Customer saved = customerRepository.save(customer);
        auditService.log(AuditAction.CUSTOMER_STATUS_CHANGED, "Customer", saved.getId(), old.name(), Status.ACTIVE.name());
        return DtoMapper.toCustomerResponse(saved);
    }

    @Transactional
    public CustomerResponse deactivate(Long id) {
        Customer customer = getCustomer(id);
        if (customer.getStatus() == Status.INACTIVE) {
            throw new BusinessRuleException("Customer is already inactive");
        }
        Status old = customer.getStatus();
        customer.setStatus(Status.INACTIVE);
        Customer saved = customerRepository.save(customer);
        deactivateCustomerMeters(saved.getId());
        auditService.log(AuditAction.CUSTOMER_STATUS_CHANGED, "Customer", saved.getId(), old.name(), Status.INACTIVE.name());
        return DtoMapper.toCustomerResponse(saved);
    }

    /** Sets all customer meters to INACTIVE when the customer is deactivated. */
    private void deactivateCustomerMeters(Long customerId) {
        meterRepository.findByCustomerId(customerId).forEach(meter -> {
            if (meter.getStatus() == MeterStatus.INACTIVE) {
                return;
            }
            MeterStatus previous = meter.getStatus();
            meter.setStatus(MeterStatus.INACTIVE);
            meterRepository.save(meter);
            auditService.log(AuditAction.METER_STATUS_CHANGED, "Meter", meter.getId(),
                    previous.name(), MeterStatus.INACTIVE.name());
        });
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return DtoMapper.toCustomerResponse(getCustomer(id));
    }

    @Transactional(readOnly = true)
    public CustomerResponse findOwn() {
        var user = SecurityUtils.currentUser().getUser();
        if (user.getCustomer() == null) {
            throw new BusinessRuleException("No customer account linked to this user");
        }
        return findById(user.getCustomer().getId());
    }

    /** Updates customer; logs status transitions. */
    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getCustomer(id);
        validateUniqueFields(id, request);
        var oldStatus = customer.getStatus();
        mapToEntity(customer, request);
        Customer saved = customerRepository.save(customer);
        if (oldStatus != saved.getStatus()) {
            auditService.log(AuditAction.CUSTOMER_STATUS_CHANGED, "Customer", saved.getId(),
                    oldStatus.name(), saved.getStatus().name());
        }
        return DtoMapper.toCustomerResponse(saved);
    }

    /** Deletes only when no active meters and no unpaid bills exist. */
    @Transactional
    public void delete(Long id) {
        Customer customer = getCustomer(id);
        if (meterRepository.findByCustomerId(id).stream()
                .anyMatch(m -> m.getStatus() == MeterStatus.ACTIVE)) {
            throw new BusinessRuleException("Cannot delete customer with active meters");
        }
        if (billRepository.findByCustomerIdOrderByBillingYearDescBillingMonthDesc(id).stream()
                .anyMatch(b -> b.getStatus() != BillStatus.PAID)) {
            throw new BusinessRuleException("Cannot delete customer with unpaid bills");
        }
        customerRepository.delete(customer);
    }

    public Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private void validateUniqueFields(Long id, CustomerRequest request) {
        customerRepository.findByNationalId(request.getNationalId()).ifPresent(existing -> {
            if (id == null || !existing.getId().equals(id)) {
                throw new BusinessRuleException("National ID already exists");
            }
        });
        customerRepository.findAll().stream()
                .filter(c -> id == null || !c.getId().equals(id))
                .forEach(c -> {
                    if (c.getEmail().equalsIgnoreCase(request.getEmail())) {
                        throw new BusinessRuleException("Email already exists");
                    }
                    if (c.getPhone().equals(request.getPhone())) {
                        throw new BusinessRuleException("Phone already exists");
                    }
                });
    }

    private Customer mapToEntity(Customer customer, CustomerRequest request) {
        customer.setFullName(request.getFullName());
        customer.setNationalId(request.getNationalId());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setStatus(request.getStatus());
        return customer;
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
