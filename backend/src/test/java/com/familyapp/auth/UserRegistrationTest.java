package com.familyapp.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.familyapp.auth.api.AuthResponse;
import com.familyapp.user.internal.persistence.FamilyEntity;
import com.familyapp.user.internal.persistence.FamilyRepository;
import com.familyapp.user.internal.persistence.UserEntity;
import com.familyapp.user.internal.persistence.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserRegistrationTest {
    private static final String PASSWORD = "secure-password-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registrationHashesPasswordAndCreatesFamily() throws Exception {
        String email = "registration-" + UUID.randomUUID() + "@example.com";
        String request = objectMapper.writeValueAsString(new RegistrationPayload(
                email, PASSWORD, "Alex", "Example Family"));

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        UserEntity user = userRepository.findById(response.userId()).orElseThrow();

        assertNotEquals(PASSWORD, user.getPasswordHash());
        assertTrue(passwordEncoder.matches(PASSWORD, user.getPasswordHash()));
        assertEquals(response.familyId(), user.getFamily().getId());
        FamilyEntity family = familyRepository.findById(response.familyId()).orElseThrow();
        assertEquals(response.userId(), family.getCreatedBy().getId());
    }

    private record RegistrationPayload(
            String email,
            String password,
            String firstName,
            String familyName) {}
}
