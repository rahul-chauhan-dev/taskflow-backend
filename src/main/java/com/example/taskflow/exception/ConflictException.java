package com.example.taskflow.exception;

public class ConflictException extends RuntimeException {

    private final String field; // which input caused it, or null

    public ConflictException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}