package com.ne.wasac.enums;

/**
 * Lifecycle status of a utility meter.
 */
public enum MeterStatus {
    /** Meter is installed and billable. */
    ACTIVE,
    /** Meter is inactive but not disconnected. */
    INACTIVE,
    /** Meter disconnected due to non-payment or admin action. */
    DISCONNECTED
}
