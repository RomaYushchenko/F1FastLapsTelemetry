package com.ua.yushchenko.f1.fastlaps.telemetry.processing.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.AbstractTelemetryEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * AOP aspect that logs every inbound Kafka event (topic, sessionUid, frame, full payload)
 * to the "inbound-events" logger so all consumer methods stay thin and avoid duplicate logging code.
 */
@Aspect
@Component
public class InboundEventLoggingAspect {

    private static final Logger INBOUND_LOG = LoggerFactory.getLogger("inbound-events");

    private final ObjectMapper objectMapper;

    public InboundEventLoggingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Around("execution(* com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer.*.consume(..))")
    public Object logInboundEvent(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        Object event = args != null && args.length > 0 ? args[0] : null;

        if (event == null) {
            return pjp.proceed();
        }

        String topic = resolveTopic(pjp);
        if (event instanceof AbstractTelemetryEvent e) {
            INBOUND_LOG.debug("Received event: topic={}, sessionUid={}, frame={}",
                    topic, e.getSessionUID(), e.getFrameIdentifier());
        } else {
            INBOUND_LOG.debug("Received event: topic={}, payloadType={}", topic, event.getClass().getSimpleName());
        }
        INBOUND_LOG.debug("payload={}", toPayloadJson(event));

        return pjp.proceed();
    }

    private static String resolveTopic(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        KafkaListener listener = method.getAnnotation(KafkaListener.class);
        if (listener != null && listener.topics().length > 0) {
            return listener.topics()[0];
        }
        return "unknown";
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
