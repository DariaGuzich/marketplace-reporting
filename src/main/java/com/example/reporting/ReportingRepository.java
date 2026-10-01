package com.example.reporting;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class ReportingRepository {

    private final JdbcClient jdbc;

    public ReportingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Сохранить событие. Если событие с таким event_id уже есть, ничего не происходит.
     * @return true — сохранено, false — это дубль
     */
    public boolean saveEvent(Event event) {
        int inserted = jdbc.sql("""
                        INSERT INTO events (event_id, account_id, event_time, shown)
                        VALUES (:eventId, :accountId, :eventTime, :shown)
                        ON CONFLICT (event_id) DO NOTHING
                        """)
                .param("eventId", event.eventId())
                .param("accountId", event.accountId())
                .param("eventTime", utc(event.timestamp()))
                .param("shown", event.shown())
                .update();
        return inserted == 1;
    }

    /**
     * Посчитать агрегаты за час [hour, hour + 1h) заново из сырых событий и записать поверх прежних.
     * Повторный запуск даёт тот же результат: значения заменяются, а не прибавляются.
     * @return сколько аккаунтов получили строку за этот час
     */
    public int aggregateHour(Instant hour) {
        return jdbc.sql("""
                        INSERT INTO hourly_stats (account_id, hour, requests, shown)
                        SELECT account_id, :hourStart, count(*), count(*) FILTER (WHERE shown)
                        FROM events
                        WHERE event_time >= :hourStart AND event_time < :hourEnd
                        GROUP BY account_id
                        ON CONFLICT (account_id, hour) DO UPDATE
                            SET requests = EXCLUDED.requests,
                                shown = EXCLUDED.shown,
                                aggregated_at = now()
                        """)
                .param("hourStart", utc(hour))
                .param("hourEnd", utc(hour.plus(Duration.ofHours(1))))
                .update();
    }

    public List<HourlyStats> findStats(String accountId) {
        return jdbc.sql("SELECT hour, requests, shown FROM hourly_stats WHERE account_id = :accountId ORDER BY hour")
                .param("accountId", accountId)
                .query((rs, rowNum) -> new HourlyStats(
                        rs.getObject("hour", OffsetDateTime.class).toInstant(),
                        rs.getLong("requests"),
                        rs.getLong("shown")))
                .list();
    }

    // Драйвер PostgreSQL принимает OffsetDateTime, но не Instant
    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
