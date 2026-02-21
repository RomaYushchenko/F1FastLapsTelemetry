# F1 FastLaps Telemetry - User Guide

> **Quick Start Guide** for running F1 FastLaps Telemetry application locally  
> **Last Updated:** February 13, 2026

---

## Prerequisites

Before starting, ensure you have installed:

1. **Java 17 or later**
   ```powershell
   java -version
   # Should show: java version "17" or higher
   ```

2. **Docker Desktop** (for Windows)
   - Download from: https://www.docker.com/products/docker-desktop
   - Ensure Docker is running (check system tray icon)

3. **Maven 3.8+**
   ```powershell
   mvn -version
   # Should show: Apache Maven 3.8.x or higher
   ```

4. **F1 2024/2025 Game** (PC version)
   - Telemetry UDP output must be enabled in game settings

5. **Node.js 20.19+ or 22.12+ and npm** (for running the Frontend locally)
   ```powershell
   node -v
   npm -v
   # Should show: Node v20.19+ or v22.12+ (Vite 7 requires this)
   ```
   On macOS you can upgrade via [nodejs.org](https://nodejs.org/) or: `brew install node@22` then use that version.

---

## Step-by-Step Setup

### Step 1: Clone and Build the Project

```powershell
# Navigate to your workspace
cd C:\Users\<YourUsername>\Work

# Clone repository (if not already cloned)
git clone <repository-url> F1FastLapsTelemetry
cd F1FastLapsTelemetry

# Build all modules
mvn clean install
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: ~2 minutes
```

---

### Step 2: Start Infrastructure (Kafka & PostgreSQL)

```powershell
# Navigate to infrastructure directory
cd infra

# Start Docker containers
docker-compose up -d
```

**What this starts:**
- ✅ Zookeeper (port 2181)
- ✅ Kafka (port 9092)
- ✅ PostgreSQL with TimescaleDB (port 5432)

**Verify containers are running:**
```powershell
docker ps
```

**Expected Output:**
```
CONTAINER ID   IMAGE                        STATUS
xxxxx          confluentinc/cp-kafka        Up
xxxxx          confluentinc/cp-zookeeper    Up
xxxxx          timescale/timescaledb        Up
```

**Wait 30 seconds** for services to fully initialize.

**Kafka topics:** Топіки створюються автоматично брокером Kafka при першій публікації (у `docker-compose.yml` ввімкнено `KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"`). Ручне створення не потрібне.

---

### Step 3: Start UDP Ingest Service

**Terminal 1:**
```powershell
cd C:\Users\<YourUsername>\Work\F1FastLapsTelemetry\udp-ingest-service

mvn spring-boot:run
```

**Expected Output:**
```
2026-02-01 10:00:00.000  INFO --- [main] UdpIngestApplication: Started UdpIngestApplication in 5.2 seconds
2026-02-01 10:00:00.100  INFO --- [main] UdpTelemetryListener: UDP listener started on port 20777
```

✅ **Service is ready** when you see: `UDP listener started on port 20777`

**Leave this terminal running!**

---

### Step 4: Start Telemetry Processing Service  
*(Документація сервісу: [telemetry_processing_api_service.md](telemetry_processing_api_service.md))*

**Terminal 2:**
```powershell
cd C:\Users\<YourUsername>\Work\F1FastLapsTelemetry\telemetry-processing-api-service

mvn spring-boot:run
```

**Expected Output:**
```
2026-02-01 10:01:00.000  INFO --- [main] TelemetryProcessingApplication: Started TelemetryProcessingApplication in 8.5 seconds
2026-02-01 10:01:00.100  INFO --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer: Tomcat started on port(s): 8080 (http)
2026-02-01 10:01:00.200  INFO --- [main] WebSocketConfig: STOMP endpoint registered at /ws/live
```

✅ **Service is ready** when you see: `Tomcat started on port(s): 8080`

**Leave this terminal running!**

---

### Step 5: Start the Frontend (React UI)

**Terminal 3:**
```powershell
cd C:\Users\<YourUsername>\Work\F1FastLapsTelemetry\ui

# Install dependencies (first time only)
npm install

# Start the development server
npm run dev
```

**Expected Output:**
```
  VITE v7.x.x  ready in xxx ms
  ➜  Local:   http://localhost:5173/
  ➜  press h + enter to show help
```

✅ **Frontend is ready** when you see the local URL (default: `http://localhost:5173`).

**Leave this terminal running!**

**Optional — override API/WebSocket URLs** (if your backend runs on a different host/port):

Create a `.env` file in the `ui/` directory:
```env
# Backend REST API base URL (default: http://localhost:8080)
VITE_API_BASE_URL=http://localhost:8080

# WebSocket URL for live telemetry (default: same as API)
VITE_WS_URL=http://localhost:8080
```

Then open **http://localhost:5173** in your browser to use the Session list, Live dashboard, and lap analysis.

---

### Step 6: Configure F1 Game Settings

1. **Start F1 2024/2025 Game**

2. **Navigate to Settings** → **Telemetry Settings**

3. **Configure UDP Telemetry:**
   - ✅ **UDP Telemetry:** ON
   - ✅ **UDP Broadcast Mode:** ON
   - ✅ **UDP IP Address:** `127.0.0.1` (localhost)
   - ✅ **UDP Port:** `20777`
   - ✅ **UDP Format:** `2024` or `2025`
   - ✅ **UDP Send Rate:** `20 Hz` (recommended) or `60 Hz` (max)

4. **Save Settings** and return to menu

---

### Step 7: Start a Racing Session

1. **Start any game mode:**
   - Practice Session
   - Qualifying
   - Race
   - Time Trial

2. **Drive your car!**
   - As soon as the session starts, telemetry data begins flowing
   - Complete at least one full lap to see aggregated data

---

## Verifying Data Flow

### Check UDP Ingest Service Logs (Terminal 1)

**You should see:**
```
INFO --- [udp-listener] SessionPacketHandler: Processing session event: SSTA, sessionUID=123456789
INFO --- [udp-listener] LapDataPacketHandler: Processing lap data for session 123456789, carIndex 0
INFO --- [udp-listener] CarTelemetryPacketHandler: Processing telemetry for session 123456789
```

✅ **UDP packets are being received and published to Kafka**

---

### Check Processing Service Logs (Terminal 2)

**You should see:**
```
INFO --- [kafka-consumer] SessionEventConsumer: Session started: sessionUID=123456789
INFO --- [kafka-consumer] LapDataConsumer: Processing lap data: sessionUID=123456789, lapNumber=1
INFO --- [kafka-consumer] SessionLifecycleService: Session started: sessionUID=123456789
INFO --- [scheduling-1] LiveDataBroadcaster: Broadcast snapshot for session 123456789
```

✅ **Data is being processed and stored in database**

---

## Accessing the Application

### Web UI (Frontend)

**URL:** `http://localhost:5173` (when the frontend is running via `npm run dev` in the `ui/` directory)

The React UI provides:
- **Session list** — view and select telemetry sessions
- **Live dashboard** — real-time speed, RPM, gear, throttle/brake via WebSocket
- **Lap analysis** — historical laps and sector times with charts

Ensure the **Telemetry Processing Service** (port 8080) is running so the UI can call the REST API and connect to the WebSocket.

---

### REST API Endpoints

**Base URL:** `http://localhost:8080`

#### 1. List All Sessions
```powershell
curl http://localhost:8080/api/sessions
```

**Response:**
```json
[
  {
    "sessionUID": 123456789,
    "trackId": 0,
    "sessionType": "Practice",
    "startedAt": "2026-02-01T10:05:00Z",
    "endedAt": null,
    "state": "ACTIVE"
  }
]
```

#### 2. Get Session Details
```powershell
curl http://localhost:8080/api/sessions/123456789
```

#### 3. Get Laps for Session
```powershell
curl "http://localhost:8080/api/sessions/123456789/laps?carIndex=0"
```

**Response:**
```json
[
  {
    "lapNumber": 1,
    "lapTimeMs": 84523,
    "sector1Ms": 28123,
    "sector2Ms": 30456,
    "sector3Ms": 25944,
    "isInvalid": false
  }
]
```

#### 4. Get Session Summary
```powershell
curl "http://localhost:8080/api/sessions/123456789/summary?carIndex=0"
```

**Response:**
```json
{
  "totalLaps": 5,
  "bestLapTimeMs": 84123,
  "bestLapNumber": 3,
  "bestSector1Ms": 27998,
  "bestSector2Ms": 30102,
  "bestSector3Ms": 25823
}
```

---

### WebSocket Live Feed (for UI Development)

**Endpoint:** `ws://localhost:8080/ws/live`

**Protocol:** STOMP over WebSocket with SockJS fallback

**Example Connection (JavaScript):**
```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

// Connect to WebSocket
const socket = new SockJS('http://localhost:8080/ws/live');
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
  console.log('Connected to WebSocket');

  // Subscribe to session updates
  stompClient.subscribe('/topic/live/123456789', (message) => {
    const snapshot = JSON.parse(message.body);
    console.log('Live snapshot:', snapshot);
    // Updates every 100ms (10 Hz)
  });

  // Send subscribe message
  stompClient.send('/app/subscribe', {}, JSON.stringify({
    sessionUID: 123456789,
    carIndex: 0
  }));
});
```

**Snapshot Message Format:**
```json
{
  "type": "SNAPSHOT",
  "timestamp": "2026-02-01T10:05:30.123Z",
  "speedKph": 285,
  "gear": 7,
  "engineRpm": 11500,
  "throttle": 0.98,
  "brake": 0.0,
  "currentLap": 2,
  "currentSector": 1
}
```

---

## Accessing the Database

### PostgreSQL Connection

```powershell
# Using psql
psql -h localhost -p 5432 -U telemetry_user -d telemetry_db

# Password: telemetry_pass
```

### Useful Queries

**View all sessions:**
```sql
SELECT session_uid, started_at, ended_at, end_reason, track_id 
FROM telemetry.sessions 
ORDER BY started_at DESC;
```

**View laps for a session:**
```sql
SELECT lap_number, lap_time_ms, sector1_time_ms, sector2_time_ms, sector3_time_ms, is_invalid
FROM telemetry.laps
WHERE session_uid = 123456789 AND car_index = 0
ORDER BY lap_number;
```

**View session summary:**
```sql
SELECT * FROM telemetry.session_summary
WHERE session_uid = 123456789 AND car_index = 0;
```

---

## Stopping the Application

### 1. Stop Services (in order)

**Stop Frontend (Terminal 3, if running):**
- Press `Ctrl+C`

**Stop Processing Service (Terminal 2):**
- Press `Ctrl+C`

**Stop UDP Ingest Service (Terminal 1):**
- Press `Ctrl+C`

### 2. Stop Infrastructure

```powershell
cd infra
docker-compose down
```

**To also remove volumes (delete all data):**
```powershell
docker-compose down -v
```

---

## Troubleshooting

### Problem: Frontend fails with `crypto.hash is not a function` or "Vite requires Node.js 20.19+ or 22.12+"

**Cause:** Node.js 18 or older is in use. Vite 7 needs Node 20.19+ or 22.12+.

**Solution — upgrade Node.js:**

- **macOS (Homebrew):**
  ```bash
  brew install node@22
  brew link --overwrite node@22
  ```
  Or use [nvm](https://github.com/nvm-sh/nvm): `nvm install 22` then `nvm use 22`.

- **Windows:** Install the LTS build (20.x or 22.x) from [nodejs.org](https://nodejs.org/).

Then in the project root: `node -v` (should show v20.19+ or v22.12+), then run `npm run dev` again in `ui/`.

---

### Problem: UDP Ingest Service won't start

**Error:** `Port 20777 is already in use`

**Solution:**
```powershell
# Find process using port 20777
netstat -ano | findstr :20777

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F
```

---

### Problem: No data in database

**Check:**
1. ✅ F1 game telemetry is enabled
2. ✅ UDP port is 20777
3. ✅ UDP IP is 127.0.0.1
4. ✅ Session has started (not in menu)
5. ✅ Check UDP Ingest logs for incoming packets

**Verify Kafka messages:**
```powershell
docker exec -it <kafka-container-id> kafka-console-consumer --bootstrap-server localhost:9092 --topic telemetry.session --from-beginning
```

---

### Problem: Processing Service crashes

**Error:** `Connection refused: Kafka`

**Solution:**
```powershell
# Verify Kafka is running
docker ps | findstr kafka

# Restart Kafka if needed
docker-compose restart kafka

# Wait 30 seconds, then restart processing service
```

---

### Problem: Database connection error

**Error:** `Connection refused: PostgreSQL`

**Solution:**
```powershell
# Check PostgreSQL is running
docker ps | findstr timescale

# View PostgreSQL logs
docker logs <postgres-container-id>

# Restart if needed
docker-compose restart postgres
```

---

## Testing Scenarios

### Scenario 1: Complete a Practice Session

1. Start services (Steps 1-5)
2. Configure F1 game (Step 5)
3. Start Practice session in F1 game
4. Complete 5-10 laps
5. Return to menu (triggers SEND event)
6. Check REST API for session summary

**Expected Result:**
- Session appears in `/api/sessions`
- All laps visible in `/api/sessions/{uid}/laps`
- Summary shows best lap and sectors

---

### Scenario 2: Live Dashboard (WebSocket)

1. Start services
2. Start F1 game session
3. Connect WebSocket client
4. Subscribe to session
5. Drive car and observe live updates

**Expected Result:**
- Snapshot messages every 100ms
- Speed, RPM, gear update in real-time
- Session ends with SESSION_ENDED message

---

### Scenario 3: Multiple Sessions

1. Complete Practice session
2. Complete Qualifying session
3. Complete Race

**Expected Result:**
- 3 different sessions in database
- Each with unique sessionUID
- All laps and summaries preserved

---

## Performance Tips

### For High-Performance Telemetry (60 Hz)

1. **Increase Kafka throughput:**
   - Edit `infra/docker-compose.yml`
   - Increase `KAFKA_NUM_NETWORK_THREADS: 8`

2. **Increase JVM heap:**
   ```powershell
   $env:MAVEN_OPTS="-Xmx2g"
   mvn spring-boot:run
   ```

3. **Use SSD for Docker volumes**
   - Improves PostgreSQL write performance

---

## Next Steps

### After Backend Setup

1. **Add Observability** (Stage 10)
   - Access metrics: `http://localhost:8080/actuator/metrics`
   - Health checks: `http://localhost:8080/actuator/health`

2. **Run React UI locally**
   - From `ui/`: `npm install` then `npm run dev`
   - Open http://localhost:5173 for Session list, Live dashboard, and lap analysis

3. **Production Deployment**
   - Package as Docker images
   - Deploy to Kubernetes/Docker Swarm
   - Configure external PostgreSQL and Kafka

---

## Quick Reference

### Ports Used

| Service | Port | Protocol |
|---------|------|----------|
| Frontend (Vite) | 5173 | HTTP |
| UDP Ingest | 20777 | UDP |
| Processing API | 8080 | HTTP |
| WebSocket | 8080 | WS |
| PostgreSQL | 5432 | TCP |
| Kafka | 9092 | TCP |
| Zookeeper | 2181 | TCP |

### Default Credentials

| Service | Username | Password |
|---------|----------|----------|
| PostgreSQL | telemetry_user | telemetry_pass |
| Database | telemetry_db | - |

### Key Directories

| Path | Purpose |
|------|---------|
| `ui/` | React frontend (Vite + TypeScript) |
| `infra/` | Docker Compose, init SQL scripts |
| `udp-ingest-service/` | UDP listener service |
| `telemetry-processing-api-service/` | Main backend API |
| `telemetry-api-contracts/` | Shared DTOs |

---

## Support & Documentation

- **Implementation Plan:** [implementation_steps_plan.md](implementation_steps_plan.md)
- **Progress Report:** [IMPLEMENTATION_PROGRESS.md](IMPLEMENTATION_PROGRESS.md)
- **Architecture:** [f_1_telemetry_project_architecture.md](f_1_telemetry_project_architecture.md)
- **API Contracts:** [rest_web_socket_api_contracts_f_1_telemetry.md](rest_web_socket_api_contracts_f_1_telemetry.md)

---

## Summary

**You should now have:**
- ✅ Infrastructure running (Kafka + PostgreSQL)
- ✅ UDP Ingest Service listening on port 20777
- ✅ Processing Service running on port 8080
- ✅ Frontend running at http://localhost:5173 (optional; run `npm run dev` in `ui/`)
- ✅ F1 game sending telemetry data
- ✅ Data flowing: Game → UDP → Kafka → Processing → Database
- ✅ REST API accessible at http://localhost:8080/api
- ✅ WebSocket available at ws://localhost:8080/ws/live

**Happy Racing! 🏎️💨**
