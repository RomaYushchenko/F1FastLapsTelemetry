package com.ua.yushchenko.f1.fastlaps.telemetry.processing.aggregation;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.LapId;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LapAggregator")
class LapAggregatorTest {

    @Mock
    private LapRepository lapRepository;
    @Mock
    private SessionSummaryAggregator summaryAggregator;
    @Mock
    private TyreWearRecorder tyreWearRecorder;

    @InjectMocks
    private LapAggregator aggregator;

    @SuppressWarnings("unchecked")
    private void putState(long sessionUid, short carIndex, LapRuntimeState state) throws Exception {
        Field f = LapAggregator.class.getDeclaredField("lapStates");
        f.setAccessible(true);
        Map<String, LapRuntimeState> map = (Map<String, LapRuntimeState>) f.get(aggregator);
        map.put(sessionUid + "-" + carIndex, state);
    }

    @Test
    @DisplayName("finalizeAllLaps зберігає останнє коло коли ще немає рядка в БД")
    void finalizeAllLaps_persistsLastLap_whenRowMissing() throws Exception {
        LapRuntimeState st = new LapRuntimeState(99L, (short) 0);
        st.setCurrentLapNumber((short) 5);
        st.setSector1TimeMs(30_000);
        st.setSector2TimeMs(31_000);
        st.setMaxCurrentLapTimeMs(95_208);
        st.setCurrentLapTimeMs(95_208);
        putState(99L, (short) 0, st);

        LapId id = new LapId(99L, (short) 0, (short) 5);
        when(lapRepository.findById(id)).thenReturn(Optional.empty());
        when(lapRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aggregator.finalizeAllLaps(99L);

        ArgumentCaptor<Lap> cap = ArgumentCaptor.forClass(Lap.class);
        verify(lapRepository).save(cap.capture());
        Lap saved = cap.getValue();
        assertThat(saved.getLapNumber()).isEqualTo((short) 5);
        assertThat(saved.getLapTimeMs()).isEqualTo(95_208);
        assertThat(saved.getSector1TimeMs()).isEqualTo(30_000);
        assertThat(saved.getSector2TimeMs()).isEqualTo(31_000);
        assertThat(saved.getSector3TimeMs()).isEqualTo(95_208 - 30_000 - 31_000);
    }

    @Test
    @DisplayName("finalizeAllLaps не дублює коло вже збережене в БД")
    void finalizeAllLaps_skips_whenLapAlreadyPersisted() throws Exception {
        LapRuntimeState st = new LapRuntimeState(98L, (short) 0);
        st.setCurrentLapNumber((short) 5);
        st.setMaxCurrentLapTimeMs(60_000);
        putState(98L, (short) 0, st);

        LapId id = new LapId(98L, (short) 0, (short) 5);
        Lap existing = Lap.builder()
                .sessionUid(98L)
                .carIndex((short) 0)
                .lapNumber((short) 5)
                .lapTimeMs(90_000)
                .sector1TimeMs(30_000)
                .sector2TimeMs(30_000)
                .sector3TimeMs(30_000)
                .isInvalid(false)
                .penaltiesSeconds((short) 0)
                .build();
        when(lapRepository.findById(id)).thenReturn(Optional.of(existing));

        aggregator.finalizeAllLaps(98L);

        verify(lapRepository, never()).save(any());
    }

    @Test
    @DisplayName("finalizeAllLaps використовує relaxed шлях коли секторів у стані немає але є max lap time")
    void finalizeAllLaps_usesRelaxedPath_whenSectorsMissingButMaxLapTime() throws Exception {
        LapRuntimeState st = new LapRuntimeState(97L, (short) 0);
        st.setCurrentLapNumber((short) 5);
        st.setMaxCurrentLapTimeMs(88_000);
        putState(97L, (short) 0, st);

        LapId id = new LapId(97L, (short) 0, (short) 5);
        when(lapRepository.findById(id)).thenReturn(Optional.empty());
        when(lapRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aggregator.finalizeAllLaps(97L);

        ArgumentCaptor<Lap> cap = ArgumentCaptor.forClass(Lap.class);
        verify(lapRepository).save(cap.capture());
        assertThat(cap.getValue().getLapTimeMs()).isEqualTo(88_000);
        assertThat(cap.getValue().getSector1TimeMs()).isEqualTo(0);
        assertThat(cap.getValue().getSector2TimeMs()).isEqualTo(0);
        assertThat(cap.getValue().getSector3TimeMs()).isEqualTo(88_000);
    }
}
