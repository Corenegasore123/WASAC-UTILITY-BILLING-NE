package com.ne.wasac.event;

/** Published after a bill is saved; email is sent after the transaction commits. */
public record BillGeneratedEvent(Long billId) {
}
