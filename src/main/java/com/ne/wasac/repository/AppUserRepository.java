package com.ne.wasac.repository;

import com.ne.wasac.enums.RoleName;
import com.ne.wasac.enums.Status;
import com.ne.wasac.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByNationalId(String nationalId);

    boolean existsByCustomer_Id(Long customerId);

    @Query("SELECT COUNT(u) FROM AppUser u JOIN u.roles r WHERE r.name = :role AND u.status = :status")
    long countByRoleAndStatus(@Param("role") RoleName role, @Param("status") Status status);

    @Query("""
            SELECT DISTINCT u FROM AppUser u LEFT JOIN u.roles r
            WHERE (:q IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR u.phoneNumber LIKE CONCAT('%', :q, '%')
                   OR u.nationalId LIKE CONCAT('%', :q, '%'))
              AND (:status IS NULL OR u.status = :status)
              AND (:role IS NULL OR r.name = :role)
              AND (:email IS NULL OR LOWER(u.email) = LOWER(:email))
            """)
    List<AppUser> search(
            @Param("q") String q,
            @Param("status") Status status,
            @Param("role") RoleName role,
            @Param("email") String email);
}
