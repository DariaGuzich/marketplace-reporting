package com.example.reporting;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Что делает запуск по расписанию — без ожидания расписания: часы «заморожены» на нужном моменте,
 * метод, который вызывает планировщик, вызывается напрямую.
 */
class AggregationJobTest {

    private final ReportingRepository repository = mock(ReportingRepository.class);

    @Test
    void scheduledRunAggregatesPreviousFullHour() {
        Clock elevenOhFive = Clock.fixed(Instant.parse("2026-10-01T11:05:00Z"), ZoneOffset.UTC);

        new AggregationJob(repository, elevenOhFive).aggregatePreviousHour();

        verify(repository).aggregateHour(Instant.parse("2026-10-01T10:00:00Z"));
    }

    @Test
    void scheduledRunJustAfterMidnightAggregatesLastHourOfPreviousDay() {
        Clock justAfterMidnight = Clock.fixed(Instant.parse("2026-10-02T00:05:00Z"), ZoneOffset.UTC);

        new AggregationJob(repository, justAfterMidnight).aggregatePreviousHour();

        verify(repository).aggregateHour(Instant.parse("2026-10-01T23:00:00Z"));
    }

    @Test
    void notWholeHourIsRejected() {
        AggregationJob job = new AggregationJob(repository, Clock.systemUTC());

        assertThatThrownBy(() -> job.aggregate(Instant.parse("2026-10-01T10:30:00Z")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
