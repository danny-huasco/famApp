package com.familyapp.consensus.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ConsensusExceptionHandler {
    @ExceptionHandler(OutingConflictException.class)
    public ResponseEntity<ApiError> handleOutingConflict(OutingConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("OUTING_TIME_CONFLICT", exception.getMessage()));
    }

    @ExceptionHandler(InvalidOutingRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(InvalidOutingRequestException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_OUTING", exception.getMessage()));
    }

    @ExceptionHandler(OutingNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(OutingNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("OUTING_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(OutingStateException.class)
    public ResponseEntity<ApiError> handleInvalidState(OutingStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("INVALID_OUTING_STATE", exception.getMessage()));
    }

    public record ApiError(String code, String message) {}
}
