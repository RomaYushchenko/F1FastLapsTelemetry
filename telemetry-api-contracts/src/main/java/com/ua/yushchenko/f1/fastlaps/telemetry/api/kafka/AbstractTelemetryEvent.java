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
        @JsonSubTypes.Type(value = CarStatusEvent.class, name = "CarStatusEvent"),
        @JsonSubTypes.Type(value = CarDamageEvent.class, name = "CarDamageEvent"),
        @JsonSubTypes.Type(value = SessionLifecycleEvent.class, name = "SessionLifecycleEvent")
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
