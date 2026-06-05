package com.ne.wasac.validation;

/**
 * Central regex patterns reused by Bean Validation annotations across DTOs.
 * Keeps name, phone, national ID, and meter number rules consistent.
 */
public final class ValidationPatterns {

    /** Letters and spaces only — rejects digits in person names. */
    public static final String LETTERS_ONLY_NAME = "^[a-zA-Z\\s]+$";

    /** Local or international phone: optional +, then 7–15 digits. */
    public static final String PHONE_NUMBER = "^(\\+[1-9]\\d{1,14}|[0-9]{7,15})$";

    /** National ID must be exactly 16 digits. */
    public static final String NATIONAL_ID = "^[0-9]{16}$";

    /** Water WM-##### or electricity EM-##### meter numbers. */
    public static final String METER_NUMBER = "^(WM|EM)-[0-9]{5}$";

    private ValidationPatterns() {
    }
}
