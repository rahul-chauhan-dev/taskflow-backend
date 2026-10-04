package com.example.taskflow.exception;

import com.example.taskflow.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- our own exceptions ----------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex,
                                                 HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), Map.of(), null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException ex,
                                                   HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), Map.of(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException ex,
                                                 HttpServletRequest request) {
        Map<String, String> fieldErrors =
                ex.getField() != null ? Map.of(ex.getField(), ex.getMessage()) : Map.of();
        return respond(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), fieldErrors, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex,
                                                      HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return respond(HttpStatus.CONFLICT, "The request conflicts with existing data.",
                request.getRequestURI(), Map.of(), null);
    }

    // Last resort: anything we did not expect
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on our side. Please try again later.",
                request.getRequestURI(), Map.of(), null);
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorized(UnauthorizedException ex,
                                                     HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), Map.of(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex,
                                                       HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, "Authentication required. Please log in.",
                request.getRequestURI(), Map.of(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex,
                                                     HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "You do not have permission to do that.",
                request.getRequestURI(), Map.of(), null);
    }

    // ---------- Spring's own exceptions: overrides from ResponseEntityExceptionHandler ----------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()); // first message per field
        }
        return respond(HttpStatus.BAD_REQUEST, "Validation failed", path(request), fieldErrors, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        log.debug("Unreadable request body: {}", ex.getMessage());
        return respond(HttpStatus.BAD_REQUEST,
                "Malformed request body. Check the JSON syntax, enum values "
                        + "(status, priority) and the date format (yyyy-MM-dd).",
                path(request), Map.of(), headers);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        String name = ex instanceof MethodArgumentTypeMismatchException m
                ? m.getName() : ex.getPropertyName();
        return respond(HttpStatus.BAD_REQUEST,
                "Invalid value '" + ex.getValue() + "' for parameter '" + name + "'.",
                path(request), Map.of(), headers);
    }

    // Everything else Spring raises (405, 404 for unknown URLs, missing parameters, ...)
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {

        String message = (body instanceof ProblemDetail pd && pd.getDetail() != null)
                ? pd.getDetail() : "Request failed";
        return respond(statusCode, message, path(request), Map.of(), headers);
    }

    // ---------- helpers ----------

    private ResponseEntity<Object> respond(HttpStatusCode status, String message, String path,
                                           Map<String, String> fieldErrors, HttpHeaders headers) {
        ApiErrorResponse body = ApiErrorResponse.of(status.value(), message, path, fieldErrors);
        return new ResponseEntity<>(body, headers, status);
    }

    private String path(WebRequest request) {
        return request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : "";
    }
}