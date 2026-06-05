package com.ne.wasac.repository;

import com.ne.wasac.enums.Status;
import com.ne.wasac.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByNationalId(String nationalId);

    boolean existsByNationalId(String nationalId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<Customer> findByEmail(String email);

    @Query("""
            SELECT c FROM Customer c
            WHERE (:q IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR c.phone LIKE CONCAT('%', :q, '%')
                   OR c.nationalId LIKE CONCAT('%', :q, '%'))
              AND (:status IS NULL OR c.status = :status)
              AND (:email IS NULL OR LOWER(c.email) = LOWER(:email))
            """)
    List<Customer> search(
            @Param("q") String q,
            @Param("status") Status status,
            @Param("email") String email);
}
