package com.payflow.shared;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(BusinessException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.getCode()) {
            case "insufficient-balance" -> HttpStatus.UNPROCESSABLE_CONTENT;
            case "email-already-registered", "idempotency-conflict" -> HttpStatus.CONFLICT;
            case "invalid-credentials" -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle("Regra de negócio não atendida");
        problem.setType(URI.create("https://payflow.dev/problems/" + exception.getCode()));
        return withCorrelationId(problem, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Recurso não encontrado");
        problem.setType(URI.create("https://payflow.dev/problems/not-found"));
        return withCorrelationId(problem, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(error -> error.getField(), error -> error.getDefaultMessage(), (a, b) -> a));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revise os campos informados.");
        problem.setTitle("Dados inválidos");
        problem.setType(URI.create("https://payflow.dev/problems/validation"));
        problem.setProperty("errors", errors);
        return withCorrelationId(problem, request);
    }

    private ProblemDetail withCorrelationId(ProblemDetail problem, HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        return problem;
    }
}
