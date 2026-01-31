package com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.exception.PacketDecodingException;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PacketHeaderDecoderTest {
    
    @Test
    void shouldDecodeValidPacketHeader() {
        // Arrange: Create a valid F1 2025 packet header
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        buffer.putShort((short) 2025);      // packetFormat
        buffer.put((byte) 25);               // gameYear
        buffer.put((byte) 1);                // gameMajorVersion
        buffer.put((byte) 5);                // gameMinorVersion
        buffer.put((byte) 1);                // packetVersion
        buffer.put((byte) 6);                // packetId (telemetry)
        buffer.putLong(123456789L);          // sessionUID
        buffer.putFloat(45.5f);              // sessionTime
        buffer.putInt(1000);                 // frameIdentifier
        buffer.putInt(1000);                 // overallFrameIdentifier
        buffer.put((byte) 0);                // playerCarIndex
        buffer.put((byte) 255);              // secondaryPlayerCarIndex
        
        buffer.flip();
        
        // Act
        PacketHeader header = PacketHeaderDecoder.decode(buffer);
        
        // Assert
        assertThat(header.getPacketFormat()).isEqualTo(2025);
        assertThat(header.getGameYear()).isEqualTo((short) 25);
        assertThat(header.getGameMajorVersion()).isEqualTo((short) 1);
        assertThat(header.getGameMinorVersion()).isEqualTo((short) 5);
        assertThat(header.getPacketVersion()).isEqualTo((short) 1);
        assertThat(header.getPacketId()).isEqualTo((short) 6);
        assertThat(header.getSessionUID()).isEqualTo(123456789L);
        assertThat(header.getSessionTime()).isEqualTo(45.5f);
        assertThat(header.getFrameIdentifier()).isEqualTo(1000L);
        assertThat(header.getOverallFrameIdentifier()).isEqualTo(1000L);
        assertThat(header.getPlayerCarIndex()).isEqualTo((short) 0);
        assertThat(header.getSecondaryPlayerCarIndex()).isEqualTo((short) 255);
        
        // Buffer position should have advanced
        assertThat(buffer.position()).isEqualTo(PacketHeaderDecoder.HEADER_SIZE);
    }
    
    @Test
    void shouldThrowExceptionWhenInsufficientData() {
        // Arrange: Buffer with only 10 bytes
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.flip();
        
        // Act & Assert
        assertThatThrownBy(() -> PacketHeaderDecoder.decode(buffer))
            .isInstanceOf(PacketDecodingException.class)
            .hasMessageContaining("Insufficient data for packet header");
    }
    
    @Test
    void shouldHandleBigEndianConversionCorrectly() {
        // Arrange: Create header data in little-endian (F1 format)
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.order(ByteOrder.LITTLE_ENDIAN);  // F1 uses little-endian
        
        buffer.putShort((short) 2025);
        buffer.put((byte) 25);
        buffer.put((byte) 1);
        buffer.put((byte) 0);
        buffer.put((byte) 1);
        buffer.put((byte) 1);  // Session packet
        buffer.putLong(999L);
        buffer.putFloat(10.0f);
        buffer.putInt(100);
        buffer.putInt(100);
        buffer.put((byte) 0);
        buffer.put((byte) 255);
        
        buffer.flip();
        
        // Act: Decoder handles byte order internally
        PacketHeader header = PacketHeaderDecoder.decode(buffer);
        
        // Assert: Values should be correctly decoded
        assertThat(header.getPacketId()).isEqualTo((short) 1);
        assertThat(header.getSessionUID()).isEqualTo(999L);
        assertThat(header.getFrameIdentifier()).isEqualTo(100L);
    }
    
    @Test
    void shouldLeaveRemainingBytesAsPayload() {
        // Arrange: Header + 50 bytes of payload
        ByteBuffer buffer = ByteBuffer.allocate(PacketHeaderDecoder.HEADER_SIZE + 50);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        // Header (29 bytes)
        buffer.putShort((short) 2025);
        buffer.put((byte) 25);
        buffer.put((byte) 1);
        buffer.put((byte) 0);
        buffer.put((byte) 1);
        buffer.put((byte) 2);  // Lap packet
        buffer.putLong(555L);
        buffer.putFloat(20.0f);
        buffer.putInt(200);
        buffer.putInt(200);
        buffer.put((byte) 0);
        buffer.put((byte) 255);
        
        // Payload (50 bytes)
        for (int i = 0; i < 50; i++) {
            buffer.put((byte) i);
        }
        
        buffer.flip();
        
        // Act
        PacketHeaderDecoder.decode(buffer);
        
        // Assert: 50 bytes of payload should remain
        assertThat(buffer.remaining()).isEqualTo(50);
        assertThat(buffer.get()).isEqualTo((byte) 0);  // First payload byte
    }
}
