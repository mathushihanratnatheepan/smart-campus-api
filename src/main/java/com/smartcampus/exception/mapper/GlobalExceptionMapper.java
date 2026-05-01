package com.smartcampus.exception.mapper;

import com.smartcampus.exception.ErrorResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable ex) {
        ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                "An unexpected error occurred. Please try again later.");
        return Response.status(500).type(MediaType.APPLICATION_JSON).entity(error).build();
    }
}
