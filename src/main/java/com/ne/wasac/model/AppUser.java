package com.ne.wasac.model;

import com.ne.wasac.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.util.HashSet;
import java.util.Set;

/**
 * System login account for staff and customers.
 * Passwords are BCrypt-hashed; mustChangePassword forces first-login reset.
 */
@Getter
@Setter
@Entity
@Table(name = "app_users")
@Check(constraints = "full_name ~ '^[a-zA-Z\\s]+$'")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name — letters only. */
    @Column(nullable = false)
    private String fullName;

    /** Unique login username (email). */
    @Column(nullable = false, unique = true)
    private String email;

    /** Local or international phone number. */
    @Column(nullable = false, unique = true)
    private String phoneNumber;

    /** Rwanda national ID — required for staff; set on customer signup via linked profile. */
    @Column(unique = true)
    private String nationalId;

    /** BCrypt password hash — never plain text. */
    @Column(nullable = false)
    private String password;

    /** ACTIVE required to authenticate; INACTIVE blocked at login. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    /** When true user must call change-password before other actions. */
    @Column(nullable = false)
    private boolean mustChangePassword = false;

    /**
     * True only while a self-signup customer awaits OTP verification.
     * Admin-deactivated accounts stay false — only admin can reactivate them.
     */
    @Column(nullable = false)
    private boolean pendingEmailVerification = false;

    /** Assigned security roles (ADMIN, OPERATOR, etc.). */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /** Linked customer profile when role is CUSTOMER. */
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
