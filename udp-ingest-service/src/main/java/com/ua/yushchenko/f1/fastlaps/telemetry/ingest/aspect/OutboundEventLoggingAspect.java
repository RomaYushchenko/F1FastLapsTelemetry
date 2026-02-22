package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that logs every outbound Kafka publish (topic, key, full payload)
 * to the "outbound-events" logger so the publisher stays thin and avoids duplicate logging code.
 */
@Aspect
@Component
public class OutboundEventLoggingAspect {

    private static final Logger OUTBOUND_LOG = LoggerFactory.getLogger("outbound-events");

    private final ObjectMapper objectMapper;

    public OutboundEventLoggingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Around("execution(* com.ua.yushchenko.f1.fastlaps.telemetry.ingest.publisher.KafkaTelemetryPublisher.publish(..))")
    public Object logOutboundEvent(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        String topic = args != null && args.length > 0 ? (String) args[0] : "?";
        String key = args != null && args.length > 1 ? (String) args[1] : "?";
        Object value = args != null && args.length > 2 ? args[2] : null;

        String payloadJson = toPayloadJson(value);
        OUTBOUND_LOG.debug("Publishing message to topic={}, key={}, payload={}", topic, key, payloadJson);

        return pjp.proceed();
    }

    private String toPayloadJson(Object value) {
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
