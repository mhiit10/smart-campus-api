package com.smartcampus.exception;

import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger log = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {
        log.severe("Unexpected error: " + e.getMessage());
        return Response.status(500)
            .entity(Map.of(
                "error", "Internal Server Error",
                "message", "An unexpected error occurred. Please contact support."
            ))
            .build();
    }
}