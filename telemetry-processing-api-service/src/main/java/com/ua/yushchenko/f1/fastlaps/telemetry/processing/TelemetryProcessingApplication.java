package com.ua.yushchenko.f1.fastlaps.telemetry.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
@EnableScheduling
public class TelemetryProcessingApplication {

    /** System property read by logback-spring.xml so each run writes to a new log file (previous run's file stays archived). */
    public static final String LOG_STARTUP_TIMESTAMP_PROP = "log.startup.timestamp";

    public static void main(String[] args) {
        System.setProperty(LOG_STARTUP_TIMESTAMP_PROP,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        SpringApplication.run(TelemetryProcessingApplication.class, args);
    }
}
