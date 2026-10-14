package com.familyapp.idempotency.application;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.idempotency.api.IdempotencyConflictException;
import com.familyapp.idempotency.internal.persistence.IdempotencyRecordEntity;
import com.familyapp.idempotency.internal.persistence.IdempotencyRecordRepository;

@Service
public class IdempotencyExecutor {
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyExecutor(
            IdempotencyRecordRepository idempotencyRecordRepository,
            ObjectMapper objectMapper) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Object execute(
            ProceedingJoinPoint invocation,
            String operation,
            FamilyPrincipal principal,
            String idempotencyKey) throws Throwable {
        String requestFingerprint = fingerprint(invocation, operation, principal, idempotencyKey);
        Optional<IdempotencyRecordEntity> existing =
                idempotencyRecordRepository.findByUserIdAndOperationAndIdempotencyKey(
                        principal.userId(), operation, idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyRecordEntity record = existing.get();
            if (!record.getRequestFingerprint().equals(requestFingerprint)) {
                throw new IdempotencyConflictException();
            }
            return deserializeResponse(invocation, record.getResponseBody());
        }

        Object response = invocation.proceed();
        if (response == null) {
            throw new IllegalStateException("Idempotent HTTP operations must return a response body.");
        }
        String responseBody = serialize(response);
        idempotencyRecordRepository.save(new IdempotencyRecordEntity(
                UUID.randomUUID(),
                principal.userId(),
                operation,
                idempotencyKey,
                requestFingerprint,
                responseBody));
        return response;
    }

    private String fingerprint(
            ProceedingJoinPoint invocation,
            String operation,
            FamilyPrincipal principal,
            String idempotencyKey) {
        List<Object> requestArguments = new ArrayList<>();
        for (Object argument : invocation.getArgs()) {
            if (argument != principal && !idempotencyKey.equals(argument)) {
                requestArguments.add(argument);
            }
        }
        byte[] request;
        try {
            request = objectMapper.writeValueAsBytes(new Fingerprint(operation, requestArguments));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not fingerprint the idempotent request.", exception);
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(request));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available for idempotency fingerprints.", exception);
        }
    }

    private Object deserializeResponse(ProceedingJoinPoint invocation, String responseBody) {
        Class<?> responseType = ((MethodSignature) invocation.getSignature()).getReturnType();
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not restore the stored idempotent response.", exception);
        }
    }

    private String serialize(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not store the idempotent response.", exception);
        }
    }

    private record Fingerprint(String operation, List<Object> requestArguments) {}
}
