package co.fixgo.request.policies;

import static co.fixgo.request.models.RequestStatus.ACCEPTED;
import static co.fixgo.request.models.RequestStatus.CANCELLED;
import static co.fixgo.request.models.RequestStatus.COMPLETED;
import static co.fixgo.request.models.RequestStatus.EXPIRED;
import static co.fixgo.request.models.RequestStatus.IN_PROGRESS;
import static co.fixgo.request.models.RequestStatus.ON_THE_WAY;
import static co.fixgo.request.models.RequestStatus.OPEN;
import static co.fixgo.request.models.RequestStatus.PENDING;
import static co.fixgo.request.security.ActorRole.ADMIN;
import static co.fixgo.request.security.ActorRole.CLIENT;
import static co.fixgo.request.security.ActorRole.PROVIDER;
import static co.fixgo.request.security.ActorRole.SERVICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.fixgo.request.exceptions.BusinessRuleException;
import co.fixgo.request.exceptions.ForbiddenOperationException;
import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.security.ActorRole;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Recorre todos los pares de estados por cada rol contra la tabla 26 del SRS v3.1, escrita aquí de forma
 * independiente de la implementación: 8 × 8 × 4 = 256 combinaciones.
 */
class RequestTransitionPolicyTest {

    private enum Outcome { ALLOWED, INVALID_TRANSITION, NOT_CANCELLABLE, FORBIDDEN }

    private record Step(RequestStatus from, RequestStatus to) {
    }

    /** Transiciones de la tabla 26 que no son cancelaciones, con quién las ejecuta. */
    private static final Map<Step, Set<ActorRole>> TABLE_26 = Map.of(
            new Step(PENDING, ACCEPTED), EnumSet.of(PROVIDER, SERVICE),
            new Step(PENDING, OPEN), EnumSet.of(CLIENT),
            new Step(OPEN, ACCEPTED), EnumSet.of(CLIENT),
            new Step(OPEN, EXPIRED), EnumSet.of(SERVICE),
            new Step(ACCEPTED, ON_THE_WAY), EnumSet.of(PROVIDER),
            new Step(ON_THE_WAY, IN_PROGRESS), EnumSet.of(PROVIDER),
            new Step(IN_PROGRESS, COMPLETED), EnumSet.of(SERVICE));

    /** Cancelaciones: cliente en los 4 primeros estados, proveedor asignado en ACCEPTED y ON_THE_WAY, admin y sistema en todo no final. */
    private static final Map<RequestStatus, Set<ActorRole>> CANCELLATION = Map.of(
            PENDING, EnumSet.of(CLIENT, ADMIN, SERVICE),
            OPEN, EnumSet.of(CLIENT, ADMIN, SERVICE),
            ACCEPTED, EnumSet.of(CLIENT, PROVIDER, ADMIN, SERVICE),
            ON_THE_WAY, EnumSet.of(CLIENT, PROVIDER, ADMIN, SERVICE),
            IN_PROGRESS, EnumSet.of(ADMIN, SERVICE));

    private final RequestTransitionPolicy policy = new RequestTransitionPolicy();

    static Stream<Arguments> everyPairForEveryActor() {
        List<Arguments> cases = new java.util.ArrayList<>();
        for (RequestStatus from : RequestStatus.values()) {
            for (RequestStatus to : RequestStatus.values()) {
                for (ActorRole actor : ActorRole.values()) {
                    cases.add(Arguments.of(from, to, actor, expected(from, to, actor)));
                }
            }
        }
        return cases.stream();
    }

    private static Outcome expected(RequestStatus from, RequestStatus to, ActorRole actor) {
        if (to == CANCELLED) {
            return CANCELLATION.getOrDefault(from, Set.of()).contains(actor)
                    ? Outcome.ALLOWED : Outcome.NOT_CANCELLABLE;
        }
        Set<ActorRole> actors = TABLE_26.get(new Step(from, to));
        if (actors == null) {
            return Outcome.INVALID_TRANSITION;
        }
        return actors.contains(actor) ? Outcome.ALLOWED : Outcome.FORBIDDEN;
    }

    @ParameterizedTest(name = "{0} -> {1} by {2}: {3}")
    @MethodSource("everyPairForEveryActor")
    void checksEveryPairOfStatesForEveryActor(RequestStatus from, RequestStatus to, ActorRole actor,
                                              Outcome outcome) {
        switch (outcome) {
            case ALLOWED -> assertThatCode(() -> policy.check(from, to, actor)).doesNotThrowAnyException();
            case INVALID_TRANSITION -> assertThatThrownBy(() -> policy.check(from, to, actor))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting("code").isEqualTo("INVALID_STATUS_TRANSITION");
            case NOT_CANCELLABLE -> assertThatThrownBy(() -> policy.check(from, to, actor))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting("code").isEqualTo("REQUEST_NOT_CANCELLABLE");
            case FORBIDDEN -> assertThatThrownBy(() -> policy.check(from, to, actor))
                    .isInstanceOf(ForbiddenOperationException.class);
        }
    }

    @Test
    void exposesWhereEachActorCanCancelFrom() {
        for (ActorRole actor : ActorRole.values()) {
            for (RequestStatus from : RequestStatus.values()) {
                assertThat(policy.cancellableFrom(actor).contains(from))
                        .as("%s cancels from %s", actor, from)
                        .isEqualTo(CANCELLATION.getOrDefault(from, Set.of()).contains(actor));
            }
        }
    }

    @Test
    void errorMessagesAreInSpanishAndNameTheStates() {
        assertThatThrownBy(() -> policy.check(COMPLETED, ACCEPTED, PROVIDER))
                .hasMessage("La solicitud no puede pasar de COMPLETED a ACCEPTED.");
        assertThatThrownBy(() -> policy.check(IN_PROGRESS, CANCELLED, CLIENT))
                .hasMessage("La solicitud ya no se puede cancelar porque está en estado IN_PROGRESS.");
    }
}
