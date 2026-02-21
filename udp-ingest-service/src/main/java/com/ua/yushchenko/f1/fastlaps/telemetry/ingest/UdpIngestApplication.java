package com.ua.yushchenko.f1.fastlaps.telemetry.ingest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@SpringBootApplication
public class UdpIngestApplication {

    public static void main(String[] args) {
        SpringApplication.run(UdpIngestApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("UDP ingest service started");
    }
}
