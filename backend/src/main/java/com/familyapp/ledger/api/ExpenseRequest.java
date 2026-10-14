package com.familyapp.ledger.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExpenseRequest(
        @NotBlank @Size(max = 180) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull UUID payerId,
        @NotEmpty List<@NotNull UUID> participantIds) {}
