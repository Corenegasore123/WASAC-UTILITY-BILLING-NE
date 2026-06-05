package com.ne.wasac.enums;

/**
 * Types of auditable actions recorded in the audit log.
 */
public enum AuditAction {
    USER_CREATED,
    USER_DELETED,
    USER_ACTIVATED,
    USER_DEACTIVATED,
    USER_ROLE_CHANGED,
    PROFILE_UPDATED,
    METER_READING_CAPTURED,
    BILL_APPROVED,
    PAYMENT_RECORDED,
    TARIFF_CREATED,
    TARIFF_UPDATED,
    METER_STATUS_CHANGED,
    CUSTOMER_STATUS_CHANGED
}
