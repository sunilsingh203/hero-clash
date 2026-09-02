package com.herobattle.controller;

import java.time.Instant;
import java.util.Map;

import com.herobattle.service.RoomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RoomException.RoomNotFound.class)
    public ProblemDetail handleNotFound(RoomException.RoomNotFound ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RoomException.RoomNotJoinable.class)
    public ProblemDetail handleConflict(RoomException.RoomNotJoinable ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("Invalid request");
        return problem(HttpStatus.BAD_REQUEST, detail);
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperties(Map.of("timestamp", Instant.now().toString()));
        return pd;
    }
}
