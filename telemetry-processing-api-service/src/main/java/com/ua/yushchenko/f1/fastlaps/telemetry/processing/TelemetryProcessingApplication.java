package com.ua.yushchenko.f1.fastlaps.telemetry.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TelemetryProcessingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelemetryProcessingApplication.class, args);
    }
}
