package com.familyapp.user.api;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("An account already exists for the supplied email address.");
    }
}
