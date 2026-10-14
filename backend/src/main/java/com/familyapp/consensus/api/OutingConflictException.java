package com.familyapp.consensus.api;

public class OutingConflictException extends RuntimeException {
    public OutingConflictException() {
        super("The proposed outing time has conflicts with too many attendees.");
    }
}
