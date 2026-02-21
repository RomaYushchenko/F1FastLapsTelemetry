package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.SESSION_UID;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionStateManager")
class SessionStateManagerTest {

    private SessionStateManager manager;

    @BeforeEach
    void setUp() {
        manager = new SessionStateManager();
    }

    @Test
    @DisplayName("get повертає null коли стан ще не створено")
    void get_returnsNull_whenNotCreated() {
        // Act
        SessionRuntimeState result = manager.get(SESSION_UID);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getOrCreate створює та повертає стан")
    void getOrCreate_createsAndReturnsState() {
        // Act
        SessionRuntimeState state = manager.getOrCreate(SESSION_UID);

        // Assert
        assertThat(state).isNotNull();
        assertThat(state.getSessionUID()).isEqualTo(SESSION_UID);
        assertThat(state.getState()).isEqualTo(SessionState.INIT);
    }

    @Test
    @DisplayName("get повертає той самий екземпляр після getOrCreate")
    void get_returnsSameInstance_afterGetOrCreate() {
        // Arrange
        SessionRuntimeState created = manager.getOrCreate(SESSION_UID);

        // Act
        SessionRuntimeState gotten = manager.get(SESSION_UID);

        // Assert
        assertThat(gotten).isSameAs(created);
    }

    @Test
    @DisplayName("close переводить стан у TERMINAL")
    void close_transitionsToTerminal() {
        // Arrange
        SessionRuntimeState state = manager.getOrCreate(SESSION_UID);
        state.transitionTo(SessionState.ACTIVE);

        // Act
        manager.close(SESSION_UID);

        // Assert
        assertThat(state.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("getAllActive не включає terminal сесії")
    void getAllActive_excludesTerminal() {
        // Arrange
        manager.getOrCreate(SESSION_UID).transitionTo(SessionState.ACTIVE);
        long otherUid = 999L;
        manager.getOrCreate(otherUid).transitionTo(SessionState.TERMINAL);

        // Act
        var active = manager.getAllActive();

        // Assert
        assertThat(active).containsOnlyKeys(SESSION_UID);
    }

    @Test
    @DisplayName("getActiveSessionCount повертає кількість не-terminal сесій")
    void getActiveSessionCount_returnsCountOfNonTerminal() {
        // Arrange
        manager.getOrCreate(SESSION_UID).transitionTo(SessionState.ACTIVE);
        manager.getOrCreate(2L).transitionTo(SessionState.ACTIVE);
        manager.getOrCreate(3L).transitionTo(SessionState.TERMINAL);

        // Act
        int count = manager.getActiveSessionCount();

        // Assert
        assertThat(count).isEqualTo(2);
    }
}
