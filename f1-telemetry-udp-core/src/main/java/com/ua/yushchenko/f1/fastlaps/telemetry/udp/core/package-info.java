/**
 * Core UDP telemetry packet handling infrastructure.
 * <p>
 * This package provides pure Java (no Spring) components for:
 * <ul>
 *   <li>UDP packet reception via DatagramChannel</li>
 *   <li>Packet header decoding</li>
 *   <li>Dispatching packets to registered consumers</li>
 * </ul>
 * <p>
 * Key interfaces:
 * <ul>
 *   <li>{@code UdpPacketConsumer} - handles packets of specific type</li>
 *   <li>{@code UdpPacketDispatcher} - routes packets to consumers</li>
 * </ul>
 */
package com.ua.yushchenko.f1.fastlaps.telemetry.udp.core;
