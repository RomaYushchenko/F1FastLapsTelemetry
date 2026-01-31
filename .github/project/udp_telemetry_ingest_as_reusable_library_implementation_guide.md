# UDP Telemetry Ingest Library

## Мета документа

Пояснити **покроково**, що саме потрібно зробити, щоб:
- винести UDP-ingest (DatagramChannel) у **окрему бібліотеку**
- підключати її до сервісів як **Spring Boot starter**
- працювати з UDP декларативно (через анотації), як з `@KafkaListener`
- ізолювати бізнес-логіку від інфраструктури

Цей документ написаний так, ніби я пояснюю завдання **розробнику в команді**.

---

## 1. Цільова архітектура (як має виглядати в результаті)

```
┌──────────────────────────┐
│ Business Service         │
│                          │
│  @F1UdpListener          │
│  class SessionHandler {  │
│    @F1PacketHandler      │
│    void onSession(...)   │
│  }                       │
└────────────▲─────────────┘
             │
┌────────────┴─────────────┐
│ f1-telemetry-udp-starter │  ← Spring Boot starter
└────────────▲─────────────┘
             │
┌────────────┴─────────────┐
│ f1-telemetry-udp-spring  │  ← annotations + wiring
└────────────▲─────────────┘
             │
┌────────────┴─────────────┐
│ f1-telemetry-udp-core    │  ← DatagramChannel, dispatcher
└──────────────────────────┘
```

**Бізнес-сервіс НЕ повинен знати:**
- що таке `DatagramChannel`
- що таке `ByteBuffer`
- як працює UDP

---

## 2. Структура модулів (що створити)

### 2.1 `f1-telemetry-udp-core`

**Тип:** pure Java (без Spring)

**Відповідальність:**
- читати UDP з порту
- декодувати `PacketHeader`
- диспатчити payload далі

**Ключові класи:**
- `UdpTelemetryListener`
- `PacketHeader`
- `PacketHeaderDecoder`
- `UdpPacketDispatcher`
- `UdpPacketConsumer`

---

### 2.2 `f1-telemetry-udp-spring`

**Тип:** Spring integration

**Відповідальність:**
- анотації (`@F1UdpListener`, `@F1PacketHandler`)
- сканування методів
- адаптація method → consumer

**Ключові класи:**
- `F1PacketHandlerPostProcessor`
- `MethodPacketHandler`
- `PacketHandlerRegistry`

---

### 2.3 `f1-telemetry-udp-starter`

**Тип:** Spring Boot starter

**Відповідальність:**
- AutoConfiguration
- підняття listener + dispatcher

---

## 3. Core модуль — що реалізувати

### 3.1 Контракт споживача пакета

```java
public interface UdpPacketConsumer {
    short packetId();
    void handle(PacketHeader header, ByteBuffer payload);
}
```

**Важливо:** core **не знає** про Spring, Kafka, бізнес.

---

### 3.2 Dispatcher

```java
public interface UdpPacketDispatcher {
    void dispatch(PacketHeader header, ByteBuffer payload);
}
```

Dispatcher — єдина точка маршрутизації.

---

### 3.3 UDP Listener (DatagramChannel)

Відповідальність:
- bind на порт
- receive datagram
- decode header
- передати в dispatcher

**Жодної бізнес-логіки тут бути не повинно.**

---

## 4. Spring модуль — як працюють анотації

### 4.1 Анотації

```java
@Target(TYPE)
@Retention(RUNTIME)
public @interface F1UdpListener {}
```

```java
@Target(METHOD)
@Retention(RUNTIME)
public @interface F1PacketHandler {
    short packetId();
}
```

---

### 4.2 Adapter (GoF Adapter)

Адаптує метод бізнес-класу до `UdpPacketConsumer`.

```java
class MethodPacketHandler implements UdpPacketConsumer {
    private final Object bean;
    private final Method method;
    private final short packetId;

    public void handle(PacketHeader h, ByteBuffer p) {
        method.invoke(bean, h, p);
    }
}
```

---

### 4.3 BeanPostProcessor (ключова частина)

Завдання:
- знайти всі `@F1UdpListener`
- знайти всі методи з `@F1PacketHandler`
- зареєструвати їх у registry

```java
class F1PacketHandlerPostProcessor implements BeanPostProcessor {
    List<UdpPacketConsumer> discovered;
}
```

---

## 5. Registry + Dispatcher binding

Registry:
- зберігає `packetId → consumer`

Dispatcher:
- викликає `consumer.handle()`

**Ні registry, ні dispatcher не знають про Spring.**

---

## 6. Kafka output (окремий concern)

### 6.1 Контракт

```java
public interface TelemetryPublisher {
    void publish(PacketHeader header, Object message);
}
```

---

### 6.2 KafkaTelemetryPublisher (base implementation)

- робить ТІЛЬКИ publish
- без retry
- без throttle

```java
class KafkaTelemetryPublisher implements TelemetryPublisher {
    KafkaTemplate<String, Object> template;
}
```

---

### 6.3 Retry + Throttle (GoF Decorator)

```java
TelemetryPublisher throttling(
    retrying(
        kafkaPublisher
    )
)
```

**ВАЖЛИВО:**
- декоратори **НЕ є Spring beans**
- ланцюг збирається ВРУЧНУ

---

## 7. ЄДИНЕ місце створення bean

```java
@Configuration
class TelemetryPublisherConfiguration {

    @Bean
    TelemetryPublisher telemetryPublisher(...) {
        return new ThrottlingPublisher(
            new RetryingPublisher(
                new KafkaTelemetryPublisher(...)
            )
        );
    }
}
```

Це критично для уникнення circular dependency.

---

## 8. Як виглядає бізнес-код

```java
@F1UdpListener
public class SessionHandler {

    @F1PacketHandler(packetId = 1)
    public void onSession(PacketHeader h, SessionPacket p) {
        // бізнес-логіка
    }
}
```

Бізнес-код:
- не знає про UDP
- не знає про Kafka
- не знає про потоки

---

## 9. Чеклист для реалізації

- [ ] створити `udp-core`
- [ ] реалізувати DatagramChannel listener
- [ ] реалізувати dispatcher
- [ ] створити Spring annotations
- [ ] реалізувати BeanPostProcessor
- [ ] зробити registry
- [ ] зробити KafkaPublisher
- [ ] зібрати decorators вручну
- [ ] оформити starter

---

## 10. Головна ідея (запамʼятати)

> **UDP ingestion — інфраструктура**  
> **Spring — glue**  
> **Бізнес — декларативний**

Якщо ти дотримаєшся цього плану — система буде:
- розширювана
- тестована
- без циклічних залежностей
- придатна для reuse у кількох сервісах

