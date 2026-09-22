package com.precisionpath.lab_service.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Returns the same {"message": "..."} body as GlobalExceptionHandler
 * for requests rejected by Spring Security.
 */
public final class JsonErrorHandlers {

    private JsonErrorHandlers() {
    }

    public static AuthenticationEntryPoint unauthorized() {
        return (request, response, exception) ->
                write(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Authentication is required");
    }

    public static AccessDeniedHandler forbidden() {
        return (request, response, exception) ->
                write(response, HttpServletResponse.SC_FORBIDDEN,
                        "You do not have permission to access this resource");
    }

    private static void write(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
