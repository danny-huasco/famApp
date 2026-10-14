package com.familyapp.ledger.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LedgerExceptionHandler {
    @ExceptionHandler(InvalidExpenseException.class)
    public ResponseEntity<ApiError> handleInvalidExpense(InvalidExpenseException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_EXPENSE", exception.getMessage()));
    }

    public record ApiError(String code, String message) {}
}
