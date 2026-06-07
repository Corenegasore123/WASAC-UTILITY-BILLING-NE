package com.ne.wasac.validation;

/**
 * Central regex patterns reused by Bean Validation annotations across DTOs.
 * Keeps name, phone, national ID, and meter number rules consistent.
 */
public final class ValidationPatterns {

    /** Letters and spaces only — rejects digits in person names. */
    public static final String LETTERS_ONLY_NAME = "^[a-zA-Z\\s]+$";

    /** Rwanda mobile: 072/073/078/079 + 7 digits, or +250 + same prefix (10 or 13 chars). */
    public static final String PHONE_NUMBER = "^(?:0|\\+250)(72|73|78|79)\\d{7}$";

    /** National ID must be exactly 16 digits. */
    public static final String NATIONAL_ID = "^[0-9]{16}$";

    /** Water WM-##### or electricity EM-##### meter numbers. */
    public static final String METER_NUMBER = "^(WM|EM)-[0-9]{5}$";

    /** Valid email address using lowercase letters only (no capitals). */
    public static final String LOWERCASE_EMAIL = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$";

    private ValidationPatterns() {
    }
}
