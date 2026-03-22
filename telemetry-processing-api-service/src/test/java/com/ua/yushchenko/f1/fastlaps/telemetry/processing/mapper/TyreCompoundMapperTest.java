package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TyreCompoundMapper")
class TyreCompoundMapperTest {

    @Test
    @DisplayName("toPersistedCompound повертає S для коду <= 17")
    void toPersistedCompound_returnsS_whenLowCode() {
        assertThat(TyreCompoundMapper.toPersistedCompound(16)).isEqualTo("S");
    }

    @Test
    @DisplayName("toPersistedCompound повертає M для коду 18–19")
    void toPersistedCompound_returnsM_whenMidCode() {
        assertThat(TyreCompoundMapper.toPersistedCompound(18)).isEqualTo("M");
        assertThat(TyreCompoundMapper.toPersistedCompound(19)).isEqualTo("M");
    }

    @Test
    @DisplayName("toPersistedCompound повертає H для коду >= 20")
    void toPersistedCompound_returnsH_whenHighCode() {
        assertThat(TyreCompoundMapper.toPersistedCompound(20)).isEqualTo("H");
    }

    @Test
    @DisplayName("toDisplayString повертає тире коли snapshot null")
    void toDisplayString_returnsDash_whenSnapshotNull() {
        assertThat(TyreCompoundMapper.toDisplayString((SessionRuntimeState.CarSnapshot) null)).isEqualTo("—");
    }

    @Test
    @DisplayName("toPersistedFromActualCompound: 16–17 → S, 18–19 → M, 20+ → H")
    void toPersistedFromActualCompound_mapsDryTyres() {
        assertThat(TyreCompoundMapper.toPersistedFromActualCompound((short) 16)).isEqualTo("S");
        assertThat(TyreCompoundMapper.toPersistedFromActualCompound((short) 17)).isEqualTo("S");
        assertThat(TyreCompoundMapper.toPersistedFromActualCompound((short) 18)).isEqualTo("M");
        assertThat(TyreCompoundMapper.toPersistedFromActualCompound((short) 19)).isEqualTo("M");
        assertThat(TyreCompoundMapper.toPersistedFromActualCompound((short) 20)).isEqualTo("H");
    }

    @Test
    @DisplayName("toPersistedFromActualCompound: Inter/Wet та невідомі коди → null")
    void toPersistedFromActualCompound_returnsNull_forInterWetAndUnknown() {
        assertThat(TyreCompoundMapper.toPersistedFromActualCompound((short) 7)).isNull();
        assertThat(TyreCompoundMapper.toPersistedFromActualCompound((short) 8)).isNull();
        assertThat(TyreCompoundMapper.toPersistedFromActualCompound((short) 5)).isNull();
        assertThat(TyreCompoundMapper.toPersistedFromActualCompound((Short) null)).isNull();
    }
}
