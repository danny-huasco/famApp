package com.familyapp.consensus.api;

public class OutingNotFoundException extends RuntimeException {
    public OutingNotFoundException() {
        super("Outing was not found.");
    }
}
