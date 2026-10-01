package com.example.reporting;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/** Агрегат за один час: сколько было ad-request и сколько из них показано. */
public record HourlyStats(
        @JsonProperty("hour") Instant hour,
        @JsonProperty("requests") long requests,
        @JsonProperty("shown") long shown) {
}
