package com.welli.wellibe.global.error;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;

public final class ApiErrorResponseWriter {

    private ApiErrorResponseWriter() {
    }

    public static void write(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message,
            String path
    ) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("""
                {"status":%d,"code":"%s","message":"%s","path":"%s"}
                """.formatted(
                status.value(),
                escape(code),
                escape(message),
                escape(path)
        ));
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
