package com.familyapp.auth.application;

import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.familyapp.auth.api.LoginRequest;
import com.familyapp.auth.api.RegisterRequest;
import com.familyapp.auth.security.JwtService;
import com.familyapp.user.api.UserAccountPort;
import com.familyapp.user.api.UserAccountPort.UserCredential;
import com.familyapp.user.api.UserAccountPort.UserIdentity;

@Service
public class AuthService {
    private final UserAccountPort userAccountPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserAccountPort userAccountPort,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userAccountPort = userAccountPort;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthSession register(RegisterRequest request) {
        UserIdentity user = userAccountPort.createFamilyAccount(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.familyName());
        return createSession(user.userId(), user.familyId());
    }

    public AuthSession login(LoginRequest request) {
        UserCredential user = userAccountPort.findCredentialsByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return createSession(user.userId(), user.familyId());
    }

    private AuthSession createSession(UUID userId, UUID familyId) {
        return new AuthSession(jwtService.issueToken(userId, familyId), userId, familyId);
    }
}
