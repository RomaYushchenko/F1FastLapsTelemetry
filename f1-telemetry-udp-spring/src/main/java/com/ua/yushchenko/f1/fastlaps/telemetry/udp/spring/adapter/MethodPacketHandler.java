package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.adapter;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.dispatcher.UdpPacketConsumer;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Adapter that bridges Spring-managed methods to the UDP packet consumer interface.
 * <p>
 * Wraps a method annotated with {@code @F1PacketHandler} and invokes it
 * when packets of matching type are received.
 * <p>
 * Supports three method signatures:
 * <ul>
 *   <li>(PacketHeader, ByteBuffer) - full parameters</li>
 *   <li>(ByteBuffer) - payload only</li>
 *   <li>(PacketHeader) - header only</li>
 * </ul>
 */
@Slf4j
public class MethodPacketHandler implements UdpPacketConsumer {
    
    private final Object bean;
    private final Method method;
    private final short packetId;
    private final MethodSignature signature;
    
    public MethodPacketHandler(Object bean, Method method, short packetId) {
        this.bean = bean;
        this.method = method;
        this.packetId = packetId;
        this.signature = detectSignature(method);
        
        // Make method accessible if needed
        method.setAccessible(true);
    }
    
    @Override
    public short packetId() {
        return packetId;
    }
    
    @Override
    public void handle(PacketHeader header, ByteBuffer payload) {
        try {
            switch (signature) {
                case HEADER_AND_PAYLOAD:
                    method.invoke(bean, header, payload);
                    break;
                case PAYLOAD_ONLY:
                    method.invoke(bean, payload);
                    break;
                case HEADER_ONLY:
                    method.invoke(bean, header);
                    break;
                default:
                    throw new IllegalStateException("Unsupported method signature: " + method);
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            log.error("Error invoking packet handler method {}.{} for packetId={}: {}", 
                      bean.getClass().getSimpleName(), 
                      method.getName(), 
                      packetId, 
                      cause.getMessage(), 
                      cause);
        } catch (IllegalAccessException e) {
            log.error("Cannot access packet handler method {}.{}: {}", 
                      bean.getClass().getSimpleName(), 
                      method.getName(), 
                      e.getMessage(), 
                      e);
        }
    }
    
    private MethodSignature detectSignature(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        
        if (paramTypes.length == 2 
            && paramTypes[0] == PacketHeader.class 
            && paramTypes[1] == ByteBuffer.class) {
            return MethodSignature.HEADER_AND_PAYLOAD;
        }
        
        if (paramTypes.length == 1 && paramTypes[0] == ByteBuffer.class) {
            return MethodSignature.PAYLOAD_ONLY;
        }
        
        if (paramTypes.length == 1 && paramTypes[0] == PacketHeader.class) {
            return MethodSignature.HEADER_ONLY;
        }
        
        throw new IllegalArgumentException(
            "Unsupported method signature for @F1PacketHandler: " + method + ". " +
            "Supported signatures: (PacketHeader, ByteBuffer), (ByteBuffer), (PacketHeader)"
        );
    }
    
    private enum MethodSignature {
        HEADER_AND_PAYLOAD,
        PAYLOAD_ONLY,
        HEADER_ONLY
    }
}
