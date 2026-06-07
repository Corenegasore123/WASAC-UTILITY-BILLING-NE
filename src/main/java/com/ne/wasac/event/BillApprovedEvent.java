package com.ne.wasac.event;

/** Published after finance approves a bill; email is sent after the transaction commits. */
public record BillApprovedEvent(Long billId) {
}
