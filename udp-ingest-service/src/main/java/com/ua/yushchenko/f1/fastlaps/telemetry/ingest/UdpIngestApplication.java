package com.ua.yushchenko.f1.fastlaps.telemetry.ingest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class UdpIngestApplication {

    /** System property read by logback-spring.xml so each run writes to a new log file (previous run's file stays archived). */
    public static final String LOG_STARTUP_TIMESTAMP_PROP = "log.startup.timestamp";

    public static void main(String[] args) {
        System.setProperty(LOG_STARTUP_TIMESTAMP_PROP,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        SpringApplication.run(UdpIngestApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("UDP ingest service started");
    }
}
