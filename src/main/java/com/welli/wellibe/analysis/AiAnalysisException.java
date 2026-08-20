package com.welli.wellibe.analysis;

/**
 * Wraps any failure talking to OpenAI (network error, non-2xx response,
 * unparseable content) so callers can catch one type and fall back.
 */
public class AiAnalysisException extends RuntimeException {

    public AiAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
