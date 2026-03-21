package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Base for Kafka telemetry event entities. Each event carries envelope metadata and a DTO payload.
 * Consumers handle these event types and read the DTO from the event.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LapDataEvent.class, name = "LapDataEvent"),
        @JsonSubTypes.Type(value = CarTelemetryEvent.class, name = "CarTelemetryEvent"),
        @JsonSubTypes.Type(value = CarTelemetryBatchEvent.class, name = "CarTelemetryBatchEvent"),
        @JsonSubTypes.Type(value = MotionEvent.class, name = "MotionEvent"),
        @JsonSubTypes.Type(value = CarStatusEvent.class, name = "CarStatusEvent"),
        @JsonSubTypes.Type(value = CarDamageEvent.class, name = "CarDamageEvent"),
        @JsonSubTypes.Type(value = SessionLifecycleEvent.class, name = "SessionLifecycleEvent"),
        @JsonSubTypes.Type(value = SessionDataEvent.class, name = "SessionDataEvent"),
        @JsonSubTypes.Type(value = EventEvent.class, name = "EventEvent"),
        @JsonSubTypes.Type(value = ParticipantsEvent.class, name = "ParticipantsEvent")
})
public abstract class AbstractTelemetryEvent {

    private int schemaVersion;
    private PacketId packetId;
    private long sessionUID;
    private int frameIdentifier;
    private float sessionTime;
    private int carIndex;
    private Instant producedAt;

    /** Payload (DTO) carried by this event. */
    public abstract Object getPayload();
}
