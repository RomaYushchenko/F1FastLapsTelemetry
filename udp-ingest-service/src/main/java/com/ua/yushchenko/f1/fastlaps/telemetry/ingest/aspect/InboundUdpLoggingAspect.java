package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

/**
 * AOP aspect that logs all inbound UDP traffic to the "inbound-udp" log file:
 * <ul>
 *   <li>When a packet handler is invoked: readable header (packetId, sessionUID, frame, etc.) and payload size.</li>
 *   <li>When a parser returns: parsed payload as JSON so content is human-readable, not raw bytes.</li>
 * </ul>
 * Keeps handlers and parsers free of repeated logging code.
 */
@Aspect
@Component
public class InboundUdpLoggingAspect {

    private static final Logger INBOUND_UDP_LOG = LoggerFactory.getLogger("inbound-udp");

    private final ObjectMapper objectMapper;

    public InboundUdpLoggingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Around("execution(* com.ua.yushchenko.f1.fastlaps.telemetry.ingest.handler.*PacketHandler.*(com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader, java.nio.ByteBuffer))")
    public Object logUdpPacket(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        PacketHeader header = args != null && args.length > 0 ? (PacketHeader) args[0] : null;
        ByteBuffer payload = args != null && args.length > 1 ? (ByteBuffer) args[1] : null;
        int payloadSize = payload != null ? payload.remaining() : 0;

        if (header != null) {
            INBOUND_UDP_LOG.debug("UDP packet: packetId={}, sessionUID={}, frame={}, sessionTime={}, playerCarIndex={}, payloadSize={} bytes",
                    header.getPacketId(), header.getSessionUID(), header.getFrameIdentifier(),
                    header.getSessionTime(), header.getPlayerCarIndex(), payloadSize);
        }

        return pjp.proceed();
    }

    @Around("execution(* com.ua.yushchenko.f1.fastlaps.telemetry.ingest.parser.*.parse(java.nio.ByteBuffer))")
    public Object logParsedPayload(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        if (!INBOUND_UDP_LOG.isDebugEnabled()) {
            return result;
        }
        String parserLabel = parserLabelFrom(pjp);
        String json = toJson(result);
        INBOUND_UDP_LOG.debug("parsed payload ({}): {}", parserLabel, json);
        return result;
    }

    private static String parserLabelFrom(ProceedingJoinPoint pjp) {
        String simpleName = ((MethodSignature) pjp.getSignature()).getDeclaringType().getSimpleName();
        // EventPacketParser -> event, SessionDataPacketParser -> sessionData, etc.
        String base = simpleName.replace("PacketParser", "");
        if (base.isEmpty()) {
            return "payload";
        }
        return base.substring(0, 1).toLowerCase() + base.substring(1);
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "<serialization error: " + e.getMessage() + ">";
        }
    }
}
