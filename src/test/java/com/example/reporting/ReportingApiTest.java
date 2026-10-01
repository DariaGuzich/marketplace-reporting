package com.example.reporting;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * События с заданными метками времени → агрегация за конкретный час (вызывается напрямую, без расписания) → отчёт.
 * Каждый тест работает со своим аккаунтом, поэтому общие часы не мешают тестам друг другу.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ReportingApiTest.Postgres.class)
class ReportingApiTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class Postgres {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18");
        }
    }

    @LocalServerPort
    int port;

    String account;

    @BeforeEach
    void newAccount() {
        account = "rep-" + UUID.randomUUID();
    }

    @Test
    void aggregatesOnlyEventsOfGivenHour() {
        sendEvent(UUID.randomUUID().toString(), "2026-10-01T09:59:59Z", true);    // предыдущий час
        sendEvent(UUID.randomUUID().toString(), "2026-10-01T10:00:00Z", true);    // начало часа — входит
        sendEvent(UUID.randomUUID().toString(), "2026-10-01T10:30:00Z", false);
        sendEvent(UUID.randomUUID().toString(), "2026-10-01T10:59:59Z", true);
        sendEvent(UUID.randomUUID().toString(), "2026-10-01T11:00:00Z", true);    // следующий час — не входит

        aggregate("2026-10-01T10:00:00Z");

        report().body("", hasSize(1))
                .body("[0].hour", equalTo("2026-10-01T10:00:00Z"))
                .body("[0].requests", equalTo(3))
                .body("[0].shown", equalTo(2));
    }

    @Test
    void duplicateEventIsStoredOnce() {
        String eventId = UUID.randomUUID().toString();

        sendEvent(eventId, "2026-10-01T12:10:00Z", true).body("stored", equalTo(true));
        sendEvent(eventId, "2026-10-01T12:10:00Z", true).body("stored", equalTo(false));   // повтор Serving
        aggregate("2026-10-01T12:00:00Z");

        report().body("[0].requests", equalTo(1));
    }

    @Test
    void rerunOfAggregationDoesNotDoubleCounts() {
        sendEvent(UUID.randomUUID().toString(), "2026-10-01T13:10:00Z", true);
        sendEvent(UUID.randomUUID().toString(), "2026-10-01T13:20:00Z", false);

        aggregate("2026-10-01T13:00:00Z");
        aggregate("2026-10-01T13:00:00Z");

        report().body("[0].requests", equalTo(2)).body("[0].shown", equalTo(1));
    }

    @Test
    void lateEventIsCountedOnlyAfterHourIsAggregatedAgain() {
        sendEvent(UUID.randomUUID().toString(), "2026-10-01T14:10:00Z", true);
        aggregate("2026-10-01T14:00:00Z");

        // Событие за 14:xx пришло, когда этот час уже посчитан
        sendEvent(UUID.randomUUID().toString(), "2026-10-01T14:50:00Z", true);
        report().body("[0].requests", equalTo(1));            // отчёт его пока не видит

        aggregate("2026-10-01T14:00:00Z");                    // повторная агрегация того же часа
        report().body("[0].requests", equalTo(2));
    }

    @Test
    void aggregationRejectsNotWholeHour() {
        request().post("/admin/aggregate?hour=2026-10-01T10:30:00Z").then().statusCode(400);
    }

    private ValidatableResponse sendEvent(String eventId, String timestamp, boolean shown) {
        return request()
                .body("""
                        {"event_id": "%s", "account_id": "%s", "timestamp": "%s", "shown": %s}
                        """.formatted(eventId, account, timestamp, shown))
                .post("/events")
                .then().statusCode(200);
    }

    private void aggregate(String hour) {
        request().post("/admin/aggregate?hour=" + hour).then().statusCode(200);
    }

    private ValidatableResponse report() {
        return request().get("/reports/{account}", account).then().statusCode(200);
    }

    private RequestSpecification request() {
        return RestAssured.given().port(port).contentType(ContentType.JSON);
    }
}
