package com.example.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Почасовая агрегация. Расписание только решает, КОГДА и за КАКОЙ час запустить;
 * вся работа — в aggregate(hour), которую можно вызвать и вручную (POST /admin/aggregate).
 */
@Component
public class AggregationJob {

    private static final Logger log = LoggerFactory.getLogger(AggregationJob.class);

    private final ReportingRepository repository;
    private final Clock clock;

    public AggregationJob(ReportingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** По расписанию: предыдущий полный час. В 11:05 — час 10:00–11:00. */
    @Scheduled(cron = "${reporting.aggregation-cron}", zone = "UTC")
    public void aggregatePreviousHour() {
        Instant previousHour = Instant.now(clock).truncatedTo(ChronoUnit.HOURS).minus(Duration.ofHours(1));
        aggregate(previousHour);
    }

    public int aggregate(Instant hour) {
        if (!hour.equals(hour.truncatedTo(ChronoUnit.HOURS))) {
            throw new IllegalArgumentException("hour must be a whole hour, e.g. 2026-10-01T10:00:00Z, got " + hour);
        }
        int accounts = repository.aggregateHour(hour);
        log.info("Aggregated hour {}: {} account(s)", hour, accounts);
        return accounts;
    }
}
