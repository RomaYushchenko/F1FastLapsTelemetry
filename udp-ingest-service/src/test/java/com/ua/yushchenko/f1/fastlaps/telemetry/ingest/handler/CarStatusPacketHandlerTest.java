package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.CarStatusPacketParser;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CarStatusPacketHandler")
class CarStatusPacketHandlerTest {

    @Mock
    private TelemetryPublisher publisher;
    @Spy
    private CarStatusPacketParser carStatusPacketParser = new CarStatusPacketParser();
    @InjectMocks
    private CarStatusPacketHandler handler;

    @Test
    @DisplayName("should parse and publish car status")
    void shouldParseAndPublishCarStatus() {
        // Arrange
        PacketHeader header = PacketHeader.builder()
                .packetFormat(2025)
                .gameYear((short) 25)
                .gameMajorVersion((short) 1)
                .gameMinorVersion((short) 0)
                .packetVersion((short) 1)
                .packetId((short) 7)
                .sessionUID(123456789L)
                .sessionTime(125.5f)
                .frameIdentifier(1500L)
                .overallFrameIdentifier(1500L)
                .playerCarIndex((short) 0)
                .secondaryPlayerCarIndex((short) 0)
                .build();

        ByteBuffer payload = createCarStatusPayload();

        // Act
        handler.handleCarStatusPacket(header, payload);

        // Assert
        ArgumentCaptor<CarStatusEvent> eventCaptor = ArgumentCaptor.forClass(CarStatusEvent.class);
        verify(publisher).publish(eq("telemetry.carStatus"), anyString(), eventCaptor.capture());

        CarStatusEvent event = eventCaptor.getValue();
        assertThat(event.getSessionUID()).isEqualTo(123456789L);
        assertThat(event.getPayload().getTractionControl()).isEqualTo(1);
        assertThat(event.getPayload().getAbs()).isEqualTo(1);
        assertThat(event.getPayload().getFuelInTank()).isEqualTo(45.5f);
        assertThat(event.getPayload().getFuelMix()).isEqualTo(2);
        assertThat(event.getPayload().getDrsAllowed()).isTrue();
        assertThat(event.getPayload().getTyresCompound()).isEqualTo(16);
        assertThat(event.getPayload().getErsStoreEnergy()).isEqualTo(2500000.0f);
    }

    private ByteBuffer createCarStatusPayload() {
        // Full 55 bytes of CarStatusData (F1 25) per CarStatusPacketParser.CAR_STATUS_DATA_SIZE_BYTES
        ByteBuffer buffer = ByteBuffer.allocate(CarStatusPacketParser.CAR_STATUS_DATA_SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 1);                    // tractionControl
        buffer.put((byte) 1);                    // abs
        buffer.put((byte) 2);                    // fuelMix
        buffer.put((byte) 55);                   // frontBrakeBias
        buffer.put((byte) 0);                    // pitLimiterStatus
        buffer.putFloat(45.5f);                  // fuelInTank
        buffer.putFloat(110.0f);                 // fuelCapacity
        buffer.putFloat(15.2f);                  // fuelRemainingLaps
        buffer.putShort((short) 12000);          // maxRPM
        buffer.putShort((short) 7000);           // idleRPM
        buffer.put((byte) 8);                    // maxGears
        buffer.put((byte) 1);                    // drsAllowed
        buffer.putShort((short) 0);             // drsActivationDistance
        buffer.put((byte) 16);                   // actualTyreCompound
        buffer.put((byte) 16);                   // visualTyreCompound
        buffer.put((byte) 5);                    // tyresAgeLaps
        buffer.put((byte) 0);                    // vehicleFIAFlags
        buffer.putFloat(750000.0f);              // enginePowerICE
        buffer.putFloat(120000.0f);              // enginePowerMGUK
        buffer.putFloat(2500000.0f);             // ersStoreEnergy
        buffer.put((byte) 0);                    // ersDeployMode
        buffer.putFloat(0.0f);                   // ersHarvestedThisLapMGUK
        buffer.putFloat(0.0f);                   // ersHarvestedThisLapMGUH
        buffer.putFloat(0.0f);                   // ersDeployedThisLap
        buffer.put((byte) 0);                    // networkPaused
        buffer.flip();
        return buffer;
    }
}
