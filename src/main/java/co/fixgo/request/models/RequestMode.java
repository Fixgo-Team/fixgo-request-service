package co.fixgo.request.models;

/**
 * Modo de la solicitud (sección 1.3 del SRS v3.0).
 */
public enum RequestMode {

    /** RF3.1: el cliente comparte su ubicación y el primer profesional que acepta la toma. */
    URGENT,

    /** RF3.2: el cliente describe el trabajo, recibe cotizaciones y elige una. */
    SCHEDULED
}
