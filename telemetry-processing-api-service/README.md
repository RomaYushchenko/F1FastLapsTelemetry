# telemetry-processing-api-service

Spring Boot сервіс, що споживає телеметрію з Kafka, агрегує дані, зберігає їх у PostgreSQL/TimescaleDB та надає REST API і WebSocket для UI.

**Повна документація сервісу:** [.github/project/telemetry_processing_api_service.md](../.github/project/telemetry_processing_api_service.md) — призначення, архітектура після рефакторингу, структура пакетів, шари та зв’язки з контрактами.

## Коротко

- **Вхід:** Kafka (топики session, lap, carTelemetry, carStatus, carDamage). UDP не приймає.
- **Вихід:** REST API (сесії, кола, summary), WebSocket live (10 Hz).
- **Зберігання:** raw-телеметрія (TimescaleDB), агрегати (laps, session_summary тощо) у PostgreSQL.

## Збірка та запуск

```bash
# З кореня репозиторію
mvn clean install -DskipTests

# Запуск сервісу (потрібні Kafka та БД)
cd telemetry-processing-api-service && mvn spring-boot:run
```

Перед запуском мають бути доступні Kafka та PostgreSQL/TimescaleDB (наприклад через `infra/docker-compose`).

## Тестування

Юніт-тести пишуться згідно [політики тестування](../.github/project/unit_testing_policy.md): **JUnit Jupiter + Mockito**, статичні дані з класу **TestData**, структура **AAA**, **@DisplayName**. Покриття коду має бути **не менше 85%** (line coverage).

```bash
# Запуск тестів
mvn -pl telemetry-processing-api-service test

# Тести + перевірка покриття (85%)
mvn -pl telemetry-processing-api-service verify
```

Звіт JaCoCo: `target/site/jacoco/index.html`.
