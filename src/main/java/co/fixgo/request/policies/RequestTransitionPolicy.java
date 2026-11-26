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

import co.fixgo.request.exceptions.BusinessRuleException;
import co.fixgo.request.exceptions.ForbiddenOperationException;
import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.security.ActorRole;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Máquina de estados de la solicitud: tabla 26 del SRS v3.1 (04-requirements/functional.md).
 * Decide si un actor puede llevar la solicitud de un estado a otro; no guarda nada.
 * El rol SERVICE representa al sistema: dispatch, technical-execution y los temporizadores.
 * No incluye la reapertura por garantía (RF4.9), bloqueada por Q-033.
 */
@Component
public class RequestTransitionPolicy {

    public static final String INVALID_TRANSITION = "INVALID_STATUS_TRANSITION";
    public static final String NOT_CANCELLABLE = "REQUEST_NOT_CANCELLABLE";

    private record Transition(RequestStatus from, RequestStatus to) {
    }

    /** Quién ejecuta cada transición que no es una cancelación. */
    private static final Map<Transition, Set<ActorRole>> ACTORS = Map.of(
            new Transition(PENDING, ACCEPTED), EnumSet.of(PROVIDER, SERVICE),
            new Transition(PENDING, OPEN), EnumSet.of(CLIENT),
            new Transition(OPEN, ACCEPTED), EnumSet.of(CLIENT),
            new Transition(OPEN, EXPIRED), EnumSet.of(SERVICE),
            new Transition(ACCEPTED, ON_THE_WAY), EnumSet.of(PROVIDER),
            new Transition(ON_THE_WAY, IN_PROGRESS), EnumSet.of(PROVIDER),
            new Transition(IN_PROGRESS, COMPLETED), EnumSet.of(SERVICE));

    /**
     * Valida el cambio de estado.
     *
     * @throws BusinessRuleException       409 INVALID_STATUS_TRANSITION si el destino no es alcanzable desde el origen,
     *                                     o 409 REQUEST_NOT_CANCELLABLE si el actor ya no puede cancelar en ese estado
     * @throws ForbiddenOperationException 403 si la transición existe pero no le corresponde a ese rol
     */
    public void check(RequestStatus from, RequestStatus to, ActorRole actor) {
        if (to == CANCELLED) {
            checkCancellation(from, actor);
            return;
        }
        if (!from.canTransitionTo(to)) {
            throw new BusinessRuleException(INVALID_TRANSITION,
                    "La solicitud no puede pasar de " + from + " a " + to + ".");
        }
        if (!ACTORS.getOrDefault(new Transition(from, to), Set.of()).contains(actor)) {
            throw new ForbiddenOperationException("Tu rol no puede cambiar la solicitud de " + from + " a " + to + ".");
        }
    }

    /** Estados desde los que el actor puede cancelar (RF3.4). */
    public Set<RequestStatus> cancellableFrom(ActorRole actor) {
        return switch (actor) {
            case CLIENT -> EnumSet.of(PENDING, OPEN, ACCEPTED, ON_THE_WAY);
            case PROVIDER -> EnumSet.of(ACCEPTED, ON_THE_WAY);
            case ADMIN, SERVICE -> EnumSet.of(PENDING, OPEN, ACCEPTED, ON_THE_WAY, IN_PROGRESS);
        };
    }

    private void checkCancellation(RequestStatus from, ActorRole actor) {
        if (!from.canTransitionTo(CANCELLED) || !cancellableFrom(actor).contains(from)) {
            throw new BusinessRuleException(NOT_CANCELLABLE,
                    "La solicitud ya no se puede cancelar porque está en estado " + from + ".");
        }
    }
}
