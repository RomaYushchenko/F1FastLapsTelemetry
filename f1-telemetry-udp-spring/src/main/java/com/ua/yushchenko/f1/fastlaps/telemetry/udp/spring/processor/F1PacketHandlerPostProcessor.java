package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.processor;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.adapter.MethodPacketHandler;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1PacketHandler;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.annotation.F1UdpListener;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.registry.PacketHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Spring {@link BeanPostProcessor} that scans beans for {@link F1UdpListener}
 * and {@link F1PacketHandler} annotations.
 * <p>
 * For each method annotated with {@code @F1PacketHandler} in an {@code @F1UdpListener} bean,
 * creates a {@link MethodPacketHandler} and registers it with the {@link PacketHandlerRegistry}.
 * <p>
 * Validates that packet IDs are unique across all handler methods.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class F1PacketHandlerPostProcessor implements BeanPostProcessor {
    
    private final PacketHandlerRegistry registry;
    private final Set<Short> registeredPacketIds = new HashSet<>();
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        
        // Check if bean is annotated with @F1UdpListener
        F1UdpListener listenerAnnotation = AnnotationUtils.findAnnotation(beanClass, F1UdpListener.class);
        if (listenerAnnotation == null) {
            return bean;
        }
        
        log.debug("Processing @F1UdpListener bean: {}", beanName);
        
        // Scan all methods for @F1PacketHandler
        for (Method method : beanClass.getDeclaredMethods()) {
            F1PacketHandler annotation = AnnotationUtils.findAnnotation(method, F1PacketHandler.class);
            if (annotation != null) {
                processHandlerMethod(bean, method, annotation);
            }
        }
        
        return bean;
    }
    
    private void processHandlerMethod(Object bean, Method method, F1PacketHandler annotation) {
        short packetId = annotation.packetId();
        
        // Validate unique packet ID
        if (registeredPacketIds.contains(packetId)) {
            throw new IllegalStateException(
                "Duplicate @F1PacketHandler for packetId=" + packetId + ". " +
                "Each packet ID must have exactly one handler method. " +
                "Method: " + bean.getClass().getSimpleName() + "." + method.getName()
            );
        }
        
        log.debug("Found @F1PacketHandler method: {}.{} for packetId={}", 
                  bean.getClass().getSimpleName(), 
                  method.getName(), 
                  packetId);
        
        // Create adapter and register
        MethodPacketHandler handler = new MethodPacketHandler(bean, method, packetId);
        registry.register(handler);
        registeredPacketIds.add(packetId);
    }
}
