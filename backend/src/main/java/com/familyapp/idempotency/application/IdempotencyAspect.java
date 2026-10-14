package com.familyapp.idempotency.application;

import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.familyapp.auth.security.FamilyPrincipal;
import com.familyapp.idempotency.api.Idempotent;
import com.familyapp.idempotency.api.InvalidIdempotencyKeyException;

@Aspect
@Component
public class IdempotencyAspect {
    private final IdempotencyExecutor idempotencyExecutor;

    public IdempotencyAspect(IdempotencyExecutor idempotencyExecutor) {
        this.idempotencyExecutor = idempotencyExecutor;
    }

    @Around("@annotation(idempotent)")
    public Object applyIdempotency(
            ProceedingJoinPoint invocation,
            Idempotent idempotent) throws Throwable {
        FamilyPrincipal principal = null;
        String idempotencyKey = null;
        for (Object argument : invocation.getArgs()) {
            if (argument instanceof FamilyPrincipal familyPrincipal) {
                principal = familyPrincipal;
            } else if (argument instanceof String key) {
                idempotencyKey = key;
            }
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return invocation.proceed();
        }
        UUID parsedKey;
        try {
            parsedKey = UUID.fromString(idempotencyKey);
        } catch (IllegalArgumentException exception) {
            throw new InvalidIdempotencyKeyException();
        }
        if (!parsedKey.toString().equalsIgnoreCase(idempotencyKey) || principal == null) {
            throw new InvalidIdempotencyKeyException();
        }
        return idempotencyExecutor.execute(
                invocation,
                idempotent.operation(),
                principal,
                parsedKey.toString());
    }
}
