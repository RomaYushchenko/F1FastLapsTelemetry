package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.registry;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.dispatcher.UdpPacketConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry that holds all packet handler consumers discovered from Spring beans.
 * <p>
 * Populated by {@link com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.processor.F1PacketHandlerPostProcessor}
 * during bean post-processing.
 * <p>
 * Thread-safe for concurrent registration and retrieval.
 */
@Slf4j
@Component
public class PacketHandlerRegistry {
    
    private final List<UdpPacketConsumer> consumers = new CopyOnWriteArrayList<>();
    
    /**
     * Registers a packet consumer.
     *
     * @param consumer packet consumer to register
     */
    public void register(UdpPacketConsumer consumer) {
        consumers.add(consumer);
        log.info("Registered packet handler: {} for packetId={}", 
                 consumer.getClass().getSimpleName(), 
                 consumer.packetId());
    }
    
    /**
     * Returns all registered consumers.
     *
     * @return unmodifiable list of consumers
     */
    public List<UdpPacketConsumer> getConsumers() {
        return Collections.unmodifiableList(new ArrayList<>(consumers));
    }
    
    /**
     * Returns the number of registered consumers.
     *
     * @return consumer count
     */
    public int getConsumerCount() {
        return consumers.size();
    }
}
