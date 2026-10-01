package com.example.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@SpringBootApplication
@EnableScheduling
public class ReportingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportingApplication.class, args);
    }

    /**
     * Источник текущего времени. Код берёт «сейчас» только отсюда, а не из Instant.now(),
     * поэтому в тесте можно подставить часы с фиксированным временем.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
