package com.ne.wasac.model;

import com.ne.wasac.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.time.LocalDate;

/**
 * Utility customer profile (water/electricity consumer).
 * Linked to an optional AppUser login for self-service.
 */
@Getter
@Setter
@Entity
@Table(name = "customers")
@Check(constraints = "full_name ~ '^[a-zA-Z\\s]+$'")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Customer full name — letters only, enforced at DB and DTO level. */
    @Column(nullable = false)
    private String fullName;

    /** Rwanda national ID — unique 16-digit identifier. */
    @Column(nullable = false, unique = true)
    private String nationalId;

    /** Contact email used for notifications. */
    @Column(nullable = false, unique = true)
    private String email;

    /** Local or international phone number. */
    @Column(nullable = false, unique = true)
    private String phone;

    /** Physical service address. */
    @Column(nullable = false)
    private String address;

    /** Optional DOB; when set customer must be 18+. */
    private LocalDate dateOfBirth;

    /** ACTIVE customers can receive bills; INACTIVE cannot. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;
}
