#!/usr/bin/env bash
# Етап 2.12: Опціональне ручне створення Kafka topics для F1 Telemetry
# Топіки за замовчуванням створюються автоматично брокером (KAFKA_AUTO_CREATE_TOPICS_ENABLE: true).
# Цей скрипт потрібен лише якщо потрібно створити топіки до запуску сервісів або з особливими параметрами.
# Topics: telemetry.session, .lap, .carTelemetry, .carStatus
# Запуск (з директорії infra): ./scripts/create-kafka-topics.sh

set -e

KAFKA_CONTAINER="${KAFKA_CONTAINER:-f1-telemetry-kafka}"
BROKER="${BROKER:-localhost:9092}"

create_topic() {
  local name=$1
  local partitions=${2:-1}
  local replication=${3:-1}
  echo "Creating topic: $name (partitions=$partitions, replication=$replication)"
  docker exec "$KAFKA_CONTAINER" kafka-topics \
    --bootstrap-server "$BROKER" \
    --create \
    --if-not-exists \
    --topic "$name" \
    --partitions "$partitions" \
    --replication-factor "$replication"
}

create_topic "telemetry.session" 1 1
create_topic "telemetry.lap" 1 1
create_topic "telemetry.carTelemetry" 1 1
create_topic "telemetry.carStatus" 1 1

echo "Done. Topics:"
docker exec "$KAFKA_CONTAINER" kafka-topics --bootstrap-server "$BROKER" --list | grep -E '^telemetry\.'
