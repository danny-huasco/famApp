package com.familyapp.calendar.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.familyapp.consensus.api.OutingNotFoundException;

@RestControllerAdvice(basePackages = "com.familyapp.calendar.api")
public class AvailabilityExceptionHandler {
    @ExceptionHandler(InvalidAvailabilityException.class)
    public ResponseEntity<Map<String, String>> handleInvalidAvailability(
            InvalidAvailabilityException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(OutingNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOutingNotFound(
            OutingNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }
}
