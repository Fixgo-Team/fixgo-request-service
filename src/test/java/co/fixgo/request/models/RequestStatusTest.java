package co.fixgo.request.models;

import static org.assertj.core.api.Assertions.assertThat;
import static co.fixgo.request.models.RequestStatus.ACCEPTED;
import static co.fixgo.request.models.RequestStatus.CANCELLED;
import static co.fixgo.request.models.RequestStatus.COMPLETED;
import static co.fixgo.request.models.RequestStatus.EXPIRED;
import static co.fixgo.request.models.RequestStatus.IN_PROGRESS;
import static co.fixgo.request.models.RequestStatus.ON_THE_WAY;
import static co.fixgo.request.models.RequestStatus.OPEN;
import static co.fixgo.request.models.RequestStatus.PENDING;

import org.junit.jupiter.api.Test;

class RequestStatusTest {

    @Test
    void allowedTargetsFollowTheDataDictionary() {
        assertThat(PENDING.allowedTargets()).containsExactlyInAnyOrder(ACCEPTED, OPEN, CANCELLED);
        assertThat(OPEN.allowedTargets()).containsExactlyInAnyOrder(ACCEPTED, EXPIRED, CANCELLED);
        assertThat(ACCEPTED.allowedTargets()).containsExactlyInAnyOrder(ON_THE_WAY, CANCELLED);
        assertThat(ON_THE_WAY.allowedTargets()).containsExactlyInAnyOrder(IN_PROGRESS, CANCELLED);
        assertThat(IN_PROGRESS.allowedTargets()).containsExactlyInAnyOrder(COMPLETED, CANCELLED);
    }

    @Test
    void finalStatesHaveNoTargets() {
        for (RequestStatus status : new RequestStatus[] {COMPLETED, CANCELLED, EXPIRED}) {
            assertThat(status.allowedTargets()).isEmpty();
            assertThat(status.isFinal()).isTrue();
        }
        assertThat(PENDING.isFinal()).isFalse();
        assertThat(IN_PROGRESS.isFinal()).isFalse();
    }

    @Test
    void canTransitionToChecksTheTargets() {
        assertThat(PENDING.canTransitionTo(OPEN)).isTrue();
        assertThat(PENDING.canTransitionTo(IN_PROGRESS)).isFalse();
        assertThat(COMPLETED.canTransitionTo(ACCEPTED)).isFalse();
    }

    @Test
    void onlyPendingAndOpenAreEditable() {
        for (RequestStatus status : RequestStatus.values()) {
            assertThat(status.isEditable()).isEqualTo(status == PENDING || status == OPEN);
        }
    }

    @Test
    void theTargetsAreACopyThatCannotCorruptTheMachine() {
        PENDING.allowedTargets().clear();

        assertThat(PENDING.allowedTargets()).isNotEmpty();
    }
}
