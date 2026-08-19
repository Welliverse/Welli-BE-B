package com.welli.wellibe.global.error;

public record ApiErrorResponse(
        int status,
        String code,
        String message,
        String path
) {
    public static ApiErrorResponse of(
            int status,
            String code,
            String message,
            String path
    ) {
        return new ApiErrorResponse(status, code, message, path);
    }
}
