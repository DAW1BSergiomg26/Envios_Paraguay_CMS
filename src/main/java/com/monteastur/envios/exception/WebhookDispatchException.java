package com.monteastur.envios.exception;

/**
 * Excepción lanzada cuando el despacho de un webhook falla de forma
 * transitoria (timeout, error de red o respuesta HTTP 5xx) tras agotar
 * los reintentos configurados en Resilience4j. Transporta el status HTTP
 * y el número de intentos para su auditoría en webhook_logs.
 */
public class WebhookDispatchException extends RuntimeException {

    private final Integer statusCode;
    private final Integer attempts;

    public WebhookDispatchException(String message, Integer statusCode, Integer attempts, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.attempts = attempts;
    }

    public WebhookDispatchException(String message, Integer statusCode, Integer attempts) {
        super(message);
        this.statusCode = statusCode;
        this.attempts = attempts;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Integer getAttempts() {
        return attempts;
    }
}
