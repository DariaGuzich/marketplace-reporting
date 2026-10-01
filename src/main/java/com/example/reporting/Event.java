package com.example.reporting;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** Событие от Serving: результат одного ad-request. timestamp — когда Serving принял решение. */
public record Event(
        @JsonProperty("event_id") @NotBlank String eventId,
        @JsonProperty("account_id") @NotBlank String accountId,
        @JsonProperty("timestamp") @NotNull Instant timestamp,
        @JsonProperty("shown") @NotNull Boolean shown) {
}
