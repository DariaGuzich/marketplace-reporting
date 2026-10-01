package com.example.reporting;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
public class ReportingController {

    private final ReportingRepository repository;
    private final AggregationJob aggregationJob;

    public ReportingController(ReportingRepository repository, AggregationJob aggregationJob) {
        this.repository = repository;
        this.aggregationJob = aggregationJob;
    }

    /** Принять событие от Serving. Дубль (тот же event_id) — тоже 200, но stored = false. */
    @PostMapping("/events")
    public EventResult receive(@Valid @RequestBody Event event) {
        return new EventResult(repository.saveEvent(event));
    }

    /** Запустить агрегацию за указанный час вручную: POST /admin/aggregate?hour=2026-10-01T10:00:00Z */
    @PostMapping("/admin/aggregate")
    public AggregationResult aggregate(@RequestParam("hour") Instant hour) {
        return new AggregationResult(hour, aggregationJob.aggregate(hour));
    }

    @GetMapping("/reports/{account_id}")
    public List<HourlyStats> report(@PathVariable("account_id") String accountId) {
        return repository.findStats(accountId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(IllegalArgumentException e) {
        return e.getMessage();
    }

    public record EventResult(@JsonProperty("stored") boolean stored) {
    }

    public record AggregationResult(@JsonProperty("hour") Instant hour, @JsonProperty("accounts") int accounts) {
    }
}
