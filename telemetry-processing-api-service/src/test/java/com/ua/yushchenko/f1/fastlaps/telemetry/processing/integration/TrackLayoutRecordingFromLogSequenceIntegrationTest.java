package com.ua.yushchenko.f1.fastlaps.telemetry.processing.integration;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventCode;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDataEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.MotionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.MotionEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionLifecycleEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer.LapDataConsumer;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer.MotionConsumer;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer.SessionEventConsumer;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TrackLayout;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TrackLayoutRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.TelemetryProcessingApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Integration test with real PostgreSQL (Testcontainers) and real beans — no mocks for processing.
 * Flow starts at Kafka consumers: we feed real events into consumers and assert only what is stored in the DB.
 *
 * <p>Replays event sequence from logs-dev (telemetry-processing-api-service, 2026-03-10):
 * <ul>
 *   <li>SessionData: trackId=3 (Sakhir), frame=0 → WAITING_FOR_LAP_START</li>
 *   <li>LapData: frame=2, lapNumber=1, lapDistance &gt; 0 (first lap started past S/F), sector=0, carPosition=1, invalid=false</li>
 *   <li>Motion frame 1577 → Lap 1 started (lapDistance from LapData), trackId=3</li>
 *   <li>Lap frame 7777 → Lap complete (deferred or immediate)</li>
 *   <li>Motion frame 7780+ → motion after deferred lap complete</li>
 * </ul>
 */
@SpringBootTest(classes = TelemetryProcessingApplication.class)
@ActiveProfiles("integrationtest")
@Testcontainers
@DisplayName("Track layout recording — integration (consumer → DB)")
@Transactional
class TrackLayoutRecordingFromLogSequenceIntegrationTest {

    private static final short TRACK_ID = 3; // Sakhir (Bahrain)
    private static final short CAR_INDEX = 0;
    /** First lap started (car past S/F line); recording starts only when lap 1 and lapDistance > 0. */
    private static final float LAP_DISTANCE_FIRST_PACKET = 10f;
    private static final int FRAME_SSTA = 0;
    private static final int FRAME_FIRST_LAP = 2;
    private static final int FRAME_MOTION_START = 1577;
    private static final int FRAME_LAP_COMPLETE = 7777;
    private static final int FRAME_MOTION_AFTER_DEFERRED = 7780;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("telemetry")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SessionEventConsumer sessionEventConsumer;
    @Autowired
    private LapDataConsumer lapDataConsumer;
    @Autowired
    private MotionConsumer motionConsumer;
    @Autowired
    private TrackLayoutRepository trackLayoutRepository;

    /** Unique session UID per test to avoid idempotency/state collision. */
    private static final AtomicInteger SESSION_UID_SEED = new AtomicInteger(0);

    private static long nextSessionUid() {
        return -6505755417077209691L + SESSION_UID_SEED.incrementAndGet();
    }

    private static Acknowledgment mockAck() {
        Acknowledgment ack = mock(Acknowledgment.class);
        return ack;
    }

    private void consumeSsta(long sessionUid) {
        SessionEventDto payload = SessionEventDto.builder()
                .eventCode(EventCode.SSTA)
                .sessionTypeId(0)
                .trackId((int) TRACK_ID)
                .totalLaps(5)
                .build();
        SessionLifecycleEvent event = SessionLifecycleEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.SESSION)
                .sessionUID(sessionUid)
                .frameIdentifier(FRAME_SSTA)
                .sessionTime(0f)
                .carIndex(CAR_INDEX)
                .producedAt(Instant.now())
                .payload(payload)
                .build();
        Acknowledgment ack = mockAck();
        sessionEventConsumer.consume(event, ack);
        verify(ack).acknowledge();
    }

    private void consumeLapData(long sessionUid, int frameId, int lapNumber, float lapDistance, boolean invalid) {
        LapDto payload = LapDto.builder()
                .lapNumber(lapNumber)
                .lapDistance(lapDistance)
                .lastLapTimeMs(null)
                .sector(0)
                .carPosition(1)
                .isInvalid(invalid)
                .build();
        LapDataEvent event = LapDataEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.LAP_DATA)
                .sessionUID(sessionUid)
                .frameIdentifier(frameId)
                .sessionTime(0f)
                .carIndex(CAR_INDEX)
                .playerCarIndex(CAR_INDEX)
                .producedAt(Instant.now())
                .payload(payload)
                .build();
        Acknowledgment ack = mockAck();
        lapDataConsumer.consume(event, ack);
        verify(ack).acknowledge();
    }

    private void consumeMotion(long sessionUid, int frameId) {
        float x = 100f + frameId * 0.5f;
        float z = 200f + frameId * 0.5f;
        MotionDto payload = new MotionDto(x, 1f, z, 0f, 0f);
        MotionEvent event = MotionEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.MOTION)
                .sessionUID(sessionUid)
                .frameIdentifier(frameId)
                .sessionTime(0f)
                .carIndex(CAR_INDEX)
                .producedAt(Instant.now())
                .payload(payload)
                .build();
        Acknowledgment ack = mockAck();
        motionConsumer.consume(event, ack);
        verify(ack).acknowledge();
    }

    @Nested
    @DisplayName("Replay: consumer flow only, assert DB")
    class ConsumerFlowThenAssertDb {

        @Test
        @DisplayName("SSTA → LapData → Motion (60) → LapData lap 2 → track persisted")
        void fullFlow_lapCompleteWithEnoughPoints_trackInDb() {
            long sessionUid = nextSessionUid();

            consumeSsta(sessionUid);
            consumeLapData(sessionUid, FRAME_FIRST_LAP, 1, LAP_DISTANCE_FIRST_PACKET, false);

            for (int i = 0; i < 60; i++) {
                consumeMotion(sessionUid, FRAME_MOTION_START + i);
            }

            consumeLapData(sessionUid, FRAME_LAP_COMPLETE, 2, 0f, false);

            TrackLayout saved = trackLayoutRepository.findById(TRACK_ID).orElse(null);
            assertThat(saved).isNotNull();
            assertThat(saved.getTrackId()).isEqualTo(TRACK_ID);
            assertThat(saved.getSource()).isEqualTo("RECORDED");
            assertThat(saved.getPoints()).isNotEmpty();
            assertThat(saved.getSessionUid()).isEqualTo(sessionUid);
        }

        @Test
        @DisplayName("SSTA → LapData → Motion (25) → LapData lap 2 → Motion (30) → track persisted after catch-up")
        void fullFlow_deferredLapComplete_thenMotionCatchesUp_trackInDb() {
            long sessionUid = nextSessionUid();

            consumeSsta(sessionUid);
            consumeLapData(sessionUid, FRAME_FIRST_LAP, 1, LAP_DISTANCE_FIRST_PACKET, false);

            for (int i = 0; i < 25; i++) {
                consumeMotion(sessionUid, FRAME_MOTION_START + i);
            }

            consumeLapData(sessionUid, FRAME_LAP_COMPLETE, 2, 0f, false);

            assertThat(trackLayoutRepository.findById(TRACK_ID)).isEmpty();

            for (int i = 0; i < 30; i++) {
                consumeMotion(sessionUid, FRAME_MOTION_AFTER_DEFERRED + i);
            }

            TrackLayout saved = trackLayoutRepository.findById(TRACK_ID).orElse(null);
            assertThat(saved).isNotNull();
            assertThat(saved.getTrackId()).isEqualTo(TRACK_ID);
            assertThat(saved.getSource()).isEqualTo("RECORDED");
            assertThat(saved.getSessionUid()).isEqualTo(sessionUid);
        }

        @Test
        @DisplayName("SSTA → LapData → Motion (60) → LapData lap 2 invalid → track still persisted")
        void fullFlow_invalidLap_trackStillPersisted() {
            long sessionUid = nextSessionUid();

            consumeSsta(sessionUid);
            consumeLapData(sessionUid, FRAME_FIRST_LAP, 1, LAP_DISTANCE_FIRST_PACKET, false);

            for (int i = 0; i < 60; i++) {
                consumeMotion(sessionUid, FRAME_MOTION_START + i);
            }

            consumeLapData(sessionUid, FRAME_LAP_COMPLETE, 2, 0f, true);

            TrackLayout saved = trackLayoutRepository.findById(TRACK_ID).orElse(null);
            assertThat(saved).isNotNull();
            assertThat(saved.getTrackId()).isEqualTo(TRACK_ID);
            assertThat(saved.getSource()).isEqualTo("RECORDED");
            assertThat(saved.getSessionUid()).isEqualTo(sessionUid);
        }
    }
}
