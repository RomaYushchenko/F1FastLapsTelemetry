package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.adapter;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodPacketHandlerTest {
    
    @Test
    void shouldInvokeMethodWithHeaderAndPayload() throws Exception {
        // Arrange
        TestHandler handler = new TestHandler();
        Method method = TestHandler.class.getMethod("handleFull", PacketHeader.class, ByteBuffer.class);
        MethodPacketHandler adapter = new MethodPacketHandler(handler, method, (short) 1);
        
        PacketHeader header = createHeader();
        ByteBuffer payload = ByteBuffer.allocate(10);
        
        // Act
        adapter.handle(header, payload);
        
        // Assert
        assertThat(handler.fullCalls.get()).isEqualTo(1);
        assertThat(handler.payloadOnlyCalls.get()).isZero();
        assertThat(handler.headerOnlyCalls.get()).isZero();
    }
    
    @Test
    void shouldInvokeMethodWithPayloadOnly() throws Exception {
        // Arrange
        TestHandler handler = new TestHandler();
        Method method = TestHandler.class.getMethod("handlePayloadOnly", ByteBuffer.class);
        MethodPacketHandler adapter = new MethodPacketHandler(handler, method, (short) 2);
        
        PacketHeader header = createHeader();
        ByteBuffer payload = ByteBuffer.allocate(10);
        
        // Act
        adapter.handle(header, payload);
        
        // Assert
        assertThat(handler.payloadOnlyCalls.get()).isEqualTo(1);
        assertThat(handler.fullCalls.get()).isZero();
        assertThat(handler.headerOnlyCalls.get()).isZero();
    }
    
    @Test
    void shouldInvokeMethodWithHeaderOnly() throws Exception {
        // Arrange
        TestHandler handler = new TestHandler();
        Method method = TestHandler.class.getMethod("handleHeaderOnly", PacketHeader.class);
        MethodPacketHandler adapter = new MethodPacketHandler(handler, method, (short) 3);
        
        PacketHeader header = createHeader();
        ByteBuffer payload = ByteBuffer.allocate(10);
        
        // Act
        adapter.handle(header, payload);
        
        // Assert
        assertThat(handler.headerOnlyCalls.get()).isEqualTo(1);
        assertThat(handler.fullCalls.get()).isZero();
        assertThat(handler.payloadOnlyCalls.get()).isZero();
    }
    
    @Test
    void shouldThrowExceptionForUnsupportedSignature() throws Exception {
        // Arrange
        TestHandler handler = new TestHandler();
        Method method = TestHandler.class.getMethod("handleInvalidSignature", String.class);
        
        // Act & Assert
        assertThatThrownBy(() -> new MethodPacketHandler(handler, method, (short) 4))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported method signature");
    }
    
    @Test
    void shouldReturnCorrectPacketId() throws Exception {
        // Arrange
        TestHandler handler = new TestHandler();
        Method method = TestHandler.class.getMethod("handleFull", PacketHeader.class, ByteBuffer.class);
        MethodPacketHandler adapter = new MethodPacketHandler(handler, method, (short) 42);
        
        // Act & Assert
        assertThat(adapter.packetId()).isEqualTo((short) 42);
    }
    
    @Test
    void shouldHandleExceptionInInvokedMethod() throws Exception {
        // Arrange
        TestHandler handler = new TestHandler();
        Method method = TestHandler.class.getMethod("handleWithException", PacketHeader.class, ByteBuffer.class);
        MethodPacketHandler adapter = new MethodPacketHandler(handler, method, (short) 5);
        
        PacketHeader header = createHeader();
        ByteBuffer payload = ByteBuffer.allocate(10);
        
        // Act: Should not throw exception, error is logged
        adapter.handle(header, payload);
        
        // Assert: Method was invoked (exception was thrown inside)
        assertThat(handler.exceptionCalls.get()).isEqualTo(1);
    }
    
    private PacketHeader createHeader() {
        return PacketHeader.builder()
            .packetFormat(2025)
            .gameYear((short) 25)
            .gameMajorVersion((short) 1)
            .gameMinorVersion((short) 0)
            .packetVersion((short) 1)
            .packetId((short) 1)
            .sessionUID(12345L)
            .sessionTime(10.0f)
            .frameIdentifier(100L)
            .overallFrameIdentifier(100L)
            .playerCarIndex((short) 0)
            .secondaryPlayerCarIndex((short) 255)
            .build();
    }
    
    public static class TestHandler {
        AtomicInteger fullCalls = new AtomicInteger(0);
        AtomicInteger payloadOnlyCalls = new AtomicInteger(0);
        AtomicInteger headerOnlyCalls = new AtomicInteger(0);
        AtomicInteger exceptionCalls = new AtomicInteger(0);
        
        public void handleFull(PacketHeader header, ByteBuffer payload) {
            fullCalls.incrementAndGet();
        }
        
        public void handlePayloadOnly(ByteBuffer payload) {
            payloadOnlyCalls.incrementAndGet();
        }
        
        public void handleHeaderOnly(PacketHeader header) {
            headerOnlyCalls.incrementAndGet();
        }
        
        public void handleInvalidSignature(String invalid) {
            // Invalid signature
        }
        
        public void handleWithException(PacketHeader header, ByteBuffer payload) {
            exceptionCalls.incrementAndGet();
            throw new RuntimeException("Test exception");
        }
    }
}
