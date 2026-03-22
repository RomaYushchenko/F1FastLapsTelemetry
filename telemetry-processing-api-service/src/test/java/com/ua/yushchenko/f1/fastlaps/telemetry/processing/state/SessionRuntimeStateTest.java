package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.ParticipantDataDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.ws.WsSnapshotMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionRuntimeState")
class SessionRuntimeStateTest {

    private SessionRuntimeState state;

    @BeforeEach
    void setUp() {
        state = new SessionRuntimeState(SESSION_UID);
    }

    @Test
    @DisplayName("початковий стан — INIT")
    void initialState_isInit() {
        // Act & Assert
        assertThat(state.getState()).isEqualTo(SessionState.INIT);
        assertThat(state.isActive()).isFalse();
        assertThat(state.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("transitionTo оновлює стан")
    void transitionTo_updatesState() {
        // Act
        state.transitionTo(SessionState.ACTIVE);

        // Assert
        assertThat(state.getState()).isEqualTo(SessionState.ACTIVE);
        assertThat(state.isActive()).isTrue();
    }

    @Test
    @DisplayName("lap watermark зберігає тільки більше значення")
    void lapWatermark_onlyIncreases() {
        // Act
        state.updateLapWatermark(0, 100);
        state.updateLapWatermark(0, 150);
        state.updateLapWatermark(0, 120); // idempotent: only increases

        // Assert
        assertThat(state.getLapWatermark(0)).isEqualTo(150);
    }

    @Test
    @DisplayName("telemetry та status watermarks незалежні від lap")
    void watermarksIndependentPerType() {
        state.updateLapWatermark(0, 100);
        state.updateTelemetryWatermark(0, 200);
        state.updateStatusWatermark(0, 50);

        assertThat(state.getLapWatermark(0)).isEqualTo(100);
        assertThat(state.getTelemetryWatermark(0)).isEqualTo(200);
        assertThat(state.getStatusWatermark(0)).isEqualTo(50);
    }

    @Test
    @DisplayName("getLapWatermark повертає 0 для невідомого carIndex")
    void getLapWatermark_returnsZero_forUnknownCar() {
        // Act
        int result = state.getLapWatermark(5);

        // Assert
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("updateSnapshot та getSnapshot зберігають snapshot")
    void updateSnapshot_andGetSnapshot() {
        // Arrange
        SessionRuntimeState.CarSnapshot snapshot = carSnapshot();

        // Act
        state.updateSnapshot(0, snapshot);

        // Assert
        assertThat(state.getSnapshot(0)).isSameAs(snapshot);
    }

    @Test
    @DisplayName("getLatestSnapshot повертає null коли snapshots порожні")
    void getLatestSnapshot_returnsNull_whenEmpty() {
        // Act
        WsSnapshotMessage result = state.getLatestSnapshot();

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getLatestSnapshot повертає повідомлення коли є snapshot")
    void getLatestSnapshot_returnsMessage_whenSnapshotPresent() {
        // Arrange
        state.updateSnapshot(0, carSnapshot());

        // Act
        WsSnapshotMessage msg = state.getLatestSnapshot();

        // Assert
        assertThat(msg).isNotNull();
        assertThat(msg.getType()).isEqualTo(WsSnapshotMessage.TYPE);
        assertThat(msg.getSpeedKph()).isEqualTo((int) SPEED_KPH);
    }

    @Test
    @DisplayName("getLatestSnapshot повертає snapshot гравця при carIndex != 0 (Practice)")
    void getLatestSnapshot_returnsPlayerSnapshot_whenCarIndexNotZero() {
        // Arrange: In Practice player can be carIndex 5; set player index and snapshot
        state.setPlayerCarIndex(5);
        state.updateSnapshot(5, carSnapshot());

        // Act
        WsSnapshotMessage msg = state.getLatestSnapshot();

        // Assert
        assertThat(msg).isNotNull();
        assertThat(msg.getSpeedKph()).isEqualTo((int) SPEED_KPH);
    }

    @Test
    @DisplayName("getLatestSnapshot повертає snapshot по playerCarIndex коли є кілька carIndex")
    void getLatestSnapshot_returnsSnapshotByPlayerCarIndex_whenMultipleCars() {
        // Arrange: two snapshots (e.g. stale car 0 and current player car 5)
        SessionRuntimeState.CarSnapshot snapshot0 = carSnapshot();
        snapshot0.setSpeedKph(100);
        state.updateSnapshot(0, snapshot0);

        SessionRuntimeState.CarSnapshot snapshot5 = carSnapshot();
        snapshot5.setSpeedKph((int) SPEED_KPH);
        state.updateSnapshot(5, snapshot5);

        state.setPlayerCarIndex(5);

        // Act
        WsSnapshotMessage msg = state.getLatestSnapshot();

        // Assert: must be player car (5), not arbitrary map order
        assertThat(msg).isNotNull();
        assertThat(msg.getSpeedKph()).isEqualTo((int) SPEED_KPH);
    }

    @Test
    @DisplayName("isTerminal true після переходу в TERMINAL")
    void isTerminal_afterTransitionToTerminal() {
        // Act
        state.transitionTo(SessionState.TERMINAL);

        // Assert
        assertThat(state.isTerminal()).isTrue();
        assertThat(state.isActive()).isFalse();
    }

    @Test
    @DisplayName("setParticipants оновлює карти імена та номери пілотів")
    void setParticipants_updatesMaps() {
        // Arrange
        ParticipantDataDto p0 = new ParticipantDataDto();
        p0.setCarIndex(0);
        p0.setRaceNumber(1);
        p0.setName(" VER ");

        ParticipantDataDto p1 = new ParticipantDataDto();
        p1.setCarIndex(1);
        p1.setRaceNumber(11);
        p1.setName("PER");

        // Act
        state.setParticipants(List.of(p0, p1), SessionRuntimeState.MAX_CARS_IN_UDP_DATA);

        // Assert
        assertThat(state.getNumActiveCars()).isEqualTo(SessionRuntimeState.MAX_CARS_IN_UDP_DATA);
        assertThat(state.getParticipantRaceNumber(0)).isEqualTo(1);
        assertThat(state.getParticipantName(0)).isEqualTo("VER");
        assertThat(state.getParticipantRaceNumber(1)).isEqualTo(11);
        assertThat(state.getParticipantName(1)).isEqualTo("PER");
    }

    @Test
    @DisplayName("setParticipants ігнорує null список")
    void setParticipants_ignoresNullList() {
        // Act
        state.setParticipants(null, 2);

        // Assert
        assertThat(state.getParticipantRaceNumber(0)).isNull();
        assertThat(state.getParticipantName(0)).isNull();
    }

    @Test
    @DisplayName("getLatestPositions повертає позиції впорядковані за carIndex та збагачені учасниками")
    void getLatestPositions_returnsOrderedAndEnriched() {
        // Arrange
        state.updatePosition(1, 10.0f, 20.0f);
        state.updatePosition(0, 5.0f, 15.0f);

        ParticipantDataDto p0 = new ParticipantDataDto();
        p0.setCarIndex(0);
        p0.setRaceNumber(33);
        p0.setName("VER");

        ParticipantDataDto p1 = new ParticipantDataDto();
        p1.setCarIndex(1);
        p1.setRaceNumber(11);
        p1.setName("PER");

        state.setParticipants(List.of(p0, p1), 2);

        // Act
        var positions = state.getLatestPositions();

        // Assert
        assertThat(positions).hasSize(2);
        assertThat(positions.get(0).getCarIndex()).isEqualTo(0);
        assertThat(positions.get(0).getWorldPosX()).isEqualTo(5.0f);
        assertThat(positions.get(0).getWorldPosZ()).isEqualTo(15.0f);
        assertThat(positions.get(0).getRacingNumber()).isEqualTo(33);
        assertThat(positions.get(0).getDriverLabel()).isEqualTo("VER");

        assertThat(positions.get(1).getCarIndex()).isEqualTo(1);
        assertThat(positions.get(1).getRacingNumber()).isEqualTo(11);
        assertThat(positions.get(1).getDriverLabel()).isEqualTo("PER");
    }

    @Test
    @DisplayName("setParticipants з неповним списком не очищає інші carIndex (numActiveCars не межа індексу)")
    void setParticipants_partialList_doesNotDropHighCarIndex() {
        // Arrange
        state.setLastCarPosition(0, 1);
        state.setLastCarPosition(1, 2);
        state.setLastCarPosition(2, 3);
        state.updatePosition(2, 99.0f, 88.0f);

        ParticipantDataDto p0 = new ParticipantDataDto();
        p0.setCarIndex(0);
        p0.setRaceNumber(1);
        p0.setName("A");

        ParticipantDataDto p1 = new ParticipantDataDto();
        p1.setCarIndex(1);
        p1.setRaceNumber(2);
        p1.setName("B");

        ParticipantDataDto p2 = new ParticipantDataDto();
        p2.setCarIndex(2);
        p2.setRaceNumber(3);
        p2.setName("C");

        // Act
        state.setParticipants(List.of(p0, p1, p2), 2);

        // Assert
        assertThat(state.getNumActiveCars()).isEqualTo(2);
        assertThat(state.getLastCarPosition(0)).isEqualTo(1);
        assertThat(state.getLastCarPosition(1)).isEqualTo(2);
        assertThat(state.getLastCarPosition(2)).isEqualTo(3);
        assertThat(state.getParticipantName(2)).isEqualTo("C");
        assertThat(state.getLatestPositions()).hasSize(1);
        assertThat(state.getLatestPositions().get(0).getCarIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("setParticipants з повною сіткою очищає ім'я/номер у порожніх слотах, lastCarPosition не чіпає")
    void setParticipants_fullGrid_clearsEmptyParticipantSlotsOnly() {
        state.setLastCarPosition(5, 2);
        List<ParticipantDataDto> grid = new ArrayList<>(SessionRuntimeState.MAX_CARS_IN_UDP_DATA);
        for (int i = 0; i < SessionRuntimeState.MAX_CARS_IN_UDP_DATA; i++) {
            ParticipantDataDto p = new ParticipantDataDto();
            p.setCarIndex(i);
            if (i == 0) {
                p.setRaceNumber(1);
                p.setName("Leader");
            }
            grid.add(p);
        }
        state.setParticipants(grid, 20);
        assertThat(state.getParticipantName(0)).isEqualTo("Leader");
        assertThat(state.getParticipantName(5)).isNull();
        assertThat(state.getLastCarPosition(5)).isEqualTo(2);
    }

    @Test
    @DisplayName("getLatestPositions включає високий carIndex якщо є world position (незалежно від numActiveCars)")
    void getLatestPositions_includesHighCarIndexWhenMotionPresent() {
        state.updatePosition(0, 1.0f, 2.0f);
        state.updatePosition(3, 9.0f, 9.0f);
        ParticipantDataDto p0 = new ParticipantDataDto();
        p0.setCarIndex(0);
        p0.setName("P0");
        state.setParticipants(List.of(p0), 2);

        var positions = state.getLatestPositions();

        assertThat(positions).hasSize(2);
        assertThat(positions.get(0).getCarIndex()).isEqualTo(0);
        assertThat(positions.get(1).getCarIndex()).isEqualTo(3);
    }

    @Test
    @DisplayName("getLatestLapDistance повертає -1 коли snapshot відсутній")
    void getLatestLapDistance_returnsMinusOne_whenSnapshotMissing() {
        // Act
        float result = state.getLatestLapDistance(0);

        // Assert
        assertThat(result).isEqualTo(-1f);
    }

    @Test
    @DisplayName("getLatestLapDistance повертає lapDistanceM із snapshot")
    void getLatestLapDistance_returnsValueFromSnapshot() {
        // Arrange
        SessionRuntimeState.CarSnapshot snapshot = carSnapshot();
        snapshot.setLapDistanceM(123.45f);
        state.updateSnapshot(0, snapshot);

        // Act
        float result = state.getLatestLapDistance(0);

        // Assert
        assertThat(result).isEqualTo(123.45f);
    }

    @Test
    @DisplayName("getLatestCarSnapshotWithCarIndex повертає null коли немає snapshot-ів")
    void getLatestCarSnapshotWithCarIndex_returnsNull_whenEmpty() {
        // Act
        Map.Entry<Integer, SessionRuntimeState.CarSnapshot> result = state.getLatestCarSnapshotWithCarIndex();

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getLatestCarSnapshotWithCarIndex повертає snapshot гравця коли playerCarIndex заданий")
    void getLatestCarSnapshotWithCarIndex_returnsPlayerSnapshot_whenPlayerSet() {
        // Arrange
        SessionRuntimeState.CarSnapshot snapshot0 = carSnapshot();
        snapshot0.setTimestamp(RAW_TS.minusSeconds(10));
        state.updateSnapshot(0, snapshot0);

        SessionRuntimeState.CarSnapshot playerSnapshot = carSnapshot();
        playerSnapshot.setTimestamp(RAW_TS);
        state.updateSnapshot(5, playerSnapshot);

        state.setPlayerCarIndex(5);

        // Act
        Map.Entry<Integer, SessionRuntimeState.CarSnapshot> result = state.getLatestCarSnapshotWithCarIndex();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getKey()).isEqualTo(5);
        assertThat(result.getValue()).isSameAs(playerSnapshot);
    }

    @Test
    @DisplayName("getLatestCarSnapshotWithCarIndex повертає snapshot з найбільшим timestamp коли playerCarIndex відсутній")
    void getLatestCarSnapshotWithCarIndex_returnsLatestByTimestamp_whenPlayerNotSet() {
        // Arrange
        SessionRuntimeState.CarSnapshot older = carSnapshot();
        older.setTimestamp(RAW_TS.minusSeconds(5));
        state.updateSnapshot(0, older);

        SessionRuntimeState.CarSnapshot newer = carSnapshot();
        newer.setTimestamp(RAW_TS);
        state.updateSnapshot(1, newer);

        // Act
        Map.Entry<Integer, SessionRuntimeState.CarSnapshot> result = state.getLatestCarSnapshotWithCarIndex();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getKey()).isEqualTo(1);
        assertThat(result.getValue()).isSameAs(newer);
    }
}

