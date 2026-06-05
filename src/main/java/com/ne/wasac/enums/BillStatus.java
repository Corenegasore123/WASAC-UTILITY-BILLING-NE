package com.ne.wasac.enums;

/**
 * Bill lifecycle from generation through payment and finance approval.
 */
public enum BillStatus {
    /** Bill generated, awaiting finance approval. */
    UNPAID,
    /** Finance approved the bill; customer may pay. */
    APPROVED,
    /** Some payment received; balance remains. */
    PARTIALLY_PAID,
    /** Fully settled. */
    PAID
}
