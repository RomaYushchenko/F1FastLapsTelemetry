package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for ProcessedPacket.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedPacketId implements Serializable {

    private Long sessionUid;
    private Integer frameIdentifier;
    private Short packetId;
    private Short carIndex;
}
