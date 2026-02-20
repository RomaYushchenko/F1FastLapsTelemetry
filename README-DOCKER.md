# F1 Fast Laps Telemetry — Docker Guide

## 🚀 Quick Start

```bash
# Build and start
docker-compose build
docker-compose up -d

# Verify
docker-compose ps
docker exec -it f1-telemetry-postgres psql -U telemetry -d telemetry -c "\dt telemetry.*"

# Open UI
open http://localhost
```

## 📋 Prerequisites

- **Docker** 20.10+ and **Docker Compose** v2.0+
- Minimum **4GB RAM**
- **F1 25 Game** on another machine in local network
- Platform support: **AMD64** and **ARM64** (Apple Silicon)

## 🏗️ Architecture

```
F1 25 Game → UDP:20777 → Docker Host
                           ├── udp-ingest-service (Java)
                           ├── telemetry-processing-api-service (Java)
                           ├── ui (React + Nginx)
                           ├── Kafka + Zookeeper
                           └── PostgreSQL + TimescaleDB
```

**Ports:**
- UI: `http://localhost` (80)
- API: `http://localhost:8081`
- UDP: `20777`

## ⚙️ F1 25 Game Configuration

1. **Find Docker machine IP:**
   ```bash
   ifconfig | grep "inet " | grep -v 127.0.0.1
   ```

2. **In game:** Settings → Telemetry Settings
   - UDP Telemetry: **ON**
   - UDP IP: **<Docker machine IP>**
   - UDP Port: **20777**
   - UDP Send Rate: **60Hz**

3. **Verify connection:**
   ```bash
   docker-compose logs -f udp-ingest-service
   # Should show: "Received UDP packet..."
   ```

4. **Check data in database:**
   ```bash
   docker exec -it f1-telemetry-postgres psql -U telemetry -d telemetry \
     -c "SELECT COUNT(*) FROM telemetry.sessions;"
   ```

## 🔄 Update After Code Changes

### Quick update (with cache, 2-5 min)
```bash
docker-compose build <service-name>
docker-compose up -d <service-name>
docker-compose logs -f <service-name>
```

### Full rebuild (no cache, 10-15 min)
```bash
docker-compose stop <service-name>
docker rmi f1-telemetry/<service-name>:latest
docker builder prune -f
docker-compose build --no-cache <service-name>
docker-compose up -d
```

### With data cleanup (15-20 min)
**⚠️ Deletes DB and Kafka!**
```bash
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

### Only configuration (docker-compose.yml)
```bash
docker-compose up -d
```

## 🗄️ Database

### Automatic initialization
On first PostgreSQL startup, scripts from `infra/init-db/` are executed:
- TimescaleDB extensions
- `telemetry` schema
- 7 tables (including hypertables)

### Recreate database
```bash
docker-compose down -v  # Deletes all data
docker-compose up -d    # Scripts run again
```

### Check tables
```bash
docker exec -it f1-telemetry-postgres psql -U telemetry -d telemetry
\dt telemetry.*
\q
```

## 🔧 Essential Commands

```bash
# Start/Stop
docker-compose up -d
docker-compose down
docker-compose restart <service-name>

# Monitoring
docker-compose ps
docker-compose logs -f <service-name>
docker stats

# Cleanup
docker-compose down -v              # Remove volumes
docker builder prune -a -f          # Clean build cache
docker system prune -a              # Full cleanup
```

## 🐛 Troubleshooting

### UDP packets not arriving
```bash
# 1. Check firewall
sudo ufw allow 20777/udp  # Linux
# macOS: System Settings → Firewall

# 2. Check UDP listener
docker-compose logs udp-ingest-service | grep "UDP listener started"

# 3. Test UDP
echo "test" | nc -u <docker-host-ip> 20777
```

### Kafka unhealthy / Cluster ID conflict
```bash
# Remove Kafka volumes
docker-compose down
docker volume rm f1-telemetry-kafka-data f1-telemetry-zookeeper-data
docker-compose up -d
```

### "no main manifest attribute" error
```bash
# Check pom.xml has executions/repackage
grep -A 5 "spring-boot-maven-plugin" */pom.xml

# Full rebuild
docker-compose down
docker rmi f1-telemetry/udp-ingest-service:latest f1-telemetry/telemetry-processing-api-service:latest
docker builder prune -a -f
docker-compose build --no-cache udp-ingest-service telemetry-processing-api-service
docker-compose up -d
```

### PostgreSQL connection errors
```bash
# Check tables
docker exec -it f1-telemetry-postgres psql -U telemetry -d telemetry -c "\dt telemetry.*"

# If tables missing
docker-compose down -v
docker-compose up -d
```

### Rollup error (ARM64)
```bash
# Already fixed in Dockerfile (Node 20 + fresh npm install)
# If issue persists:
docker builder prune -a -f
docker-compose build --no-cache ui
```

### Out of memory
```bash
# Check usage
docker stats

# Increase limits in docker-compose.yml or reduce JAVA_OPTS
```

## 💻 Development Workflow

### Typical cycle
```bash
# 1. Make code changes
# 2. Rebuild
docker-compose build <service-name>
# 3. Restart
docker-compose up -d <service-name>
# 4. Check logs
docker-compose logs -f <service-name>
```

### Git workflow
```bash
git checkout feature/new-feature
docker-compose build --no-cache
docker-compose up -d
```

### Export/Import images
```bash
# Export
docker save -o f1-telemetry.tar f1-telemetry/udp-ingest-service:latest \
  f1-telemetry/telemetry-processing-api-service:latest f1-telemetry/ui:latest

# Import on another machine
docker load -i f1-telemetry.tar
docker-compose up -d
```

## 🔐 Production

### Security checklist
- Change DB passwords in `docker-compose.yml` and `application.yml`
- Don't expose PostgreSQL/Kafka ports externally
- Use Docker secrets for sensitive data
- Configure HTTPS via reverse proxy

### Resource limits
Already configured in `docker-compose.yml`:
- Backend: 512MB
- Infrastructure: Kafka 2GB, PostgreSQL 1GB

## 📊 Monitoring

```bash
# Health checks
docker-compose ps

# Metrics
docker stats --no-stream

# Kafka topics
docker exec -it f1-telemetry-kafka kafka-topics --bootstrap-server localhost:29092 --list

# PostgreSQL
docker exec -it f1-telemetry-postgres psql -U telemetry -d telemetry \
  -c "SELECT COUNT(*) FROM telemetry.sessions;"
```

## 📚 Project Structure

```
F1FastLapsTelemetry/
├── docker-compose.yml              # Main compose file
├── README-DOCKER.md                # This documentation
├── udp-ingest-service/
│   ├── Dockerfile                  # Multi-stage build
│   └── src/main/resources/
│       └── application.yml         # Spring Boot config
├── telemetry-processing-api-service/
│   ├── Dockerfile
│   └── src/main/resources/
│       └── application.yml
├── ui/
│   ├── Dockerfile                  # Node build + Nginx
│   └── nginx.conf                  # Reverse proxy config
└── infra/
    └── init-db/                    # PostgreSQL init scripts
        ├── 01-extensions.sql
        ├── 02-schema.sql
        └── ...
```

## 🆘 Troubleshooting

### Kafka: `InconsistentClusterIdException` — cluster ID doesn't match

**Symptom:** Kafka container exits with:
```text
InconsistentClusterIdException: The Cluster ID ... doesn't match stored clusterId ... in meta.properties.
The broker is trying to join the wrong cluster.
```

**Cause:** Zookeeper was recreated (new container or volume) and has a new cluster ID, but Kafka's data volume still has the old cluster ID in `meta.properties`. This often happens after `docker-compose down -v` on Zookeeper only, or after removing/recreating the Zookeeper container.

**Fix:** Remove Kafka's data volume so Kafka starts fresh and creates new data that matches the current Zookeeper. **This deletes all Kafka topics and messages.**

```bash
docker-compose down
docker volume rm f1-telemetry-kafka-data
docker-compose up -d
```

If the volume name differs, list volumes and remove the Kafka one:
```bash
docker volume ls | grep kafka
docker volume rm <kafka-volume-name>
```

Then start again: `docker-compose up -d`.

---

## 🆘 Support

If you encounter issues:
1. Check logs: `docker-compose logs`
2. Check status: `docker-compose ps`
3. Check resources: `docker stats`
4. Create an issue with logs

---

**Version:** 1.0.0  
**Last updated:** 2026-02-14
