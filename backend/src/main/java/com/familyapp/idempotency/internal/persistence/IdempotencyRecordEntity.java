package com.familyapp.idempotency.internal.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "api_idempotency_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_idempotency_user_operation_key",
                columnNames = {"user_id_fk", "operation", "idempotency_key"}))
public class IdempotencyRecordEntity {
    @Id
    private UUID id;

    @Column(name = "user_id_fk", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 80)
    private String operation;

    @Column(name = "idempotency_key", nullable = false, length = 36)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "response_body", nullable = false, columnDefinition = "text")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyRecordEntity() {}

    public IdempotencyRecordEntity(
            UUID id,
            UUID userId,
            String operation,
            String idempotencyKey,
            String requestFingerprint,
            String responseBody) {
        this.id = id;
        this.userId = userId;
        this.operation = operation;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.responseBody = responseBody;
        this.createdAt = Instant.now();
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
