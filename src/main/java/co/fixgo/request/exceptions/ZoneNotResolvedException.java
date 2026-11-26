package co.fixgo.request.exceptions;

import java.util.Map;

/**
 * dispatch-matchmaking no pudo resolver la zona del punto de una urgencia (RNF-12, RNF-14).
 * Se responde 400 ZONE_NOT_RESOLVED para que la app le pida al cliente el municipio y el barrio (zoneId).
 */
public class ZoneNotResolvedException extends InvalidRequestDataException {

    public static final String CODE = "ZONE_NOT_RESOLVED";

    public ZoneNotResolvedException() {
        super(CODE, "No pudimos ubicar tu zona. Indica el municipio y el barrio para continuar.",
                Map.of("zoneId", "La zona es obligatoria cuando no se puede resolver desde la ubicación."));
    }
}
