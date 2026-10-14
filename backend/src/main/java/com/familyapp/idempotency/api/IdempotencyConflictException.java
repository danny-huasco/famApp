package com.familyapp.idempotency.api;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super("An idempotency key cannot be reused for a different request.");
    }
}
