package com.ua.yushchenko.f1.fastlaps.telemetry.ingest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UdpIngestApplication {

    public static void main(String[] args) {
        SpringApplication.run(UdpIngestApplication.class, args);
    }
}
