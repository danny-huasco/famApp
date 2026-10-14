package com.familyapp.idempotency.api;

public class InvalidIdempotencyKeyException extends RuntimeException {
    public InvalidIdempotencyKeyException() {
        super("Idempotency-Key must be a canonical UUID.");
    }
}
