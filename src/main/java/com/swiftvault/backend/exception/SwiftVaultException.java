package com.swiftvault.backend.exception;

import org.springframework.http.HttpStatus;

public class SwiftVaultException extends RuntimeException {

    private final HttpStatus status;

    public SwiftVaultException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static SwiftVaultException badRequest(String message)   { return new SwiftVaultException(message, HttpStatus.BAD_REQUEST); }
    public static SwiftVaultException unauthorized(String message)  { return new SwiftVaultException(message, HttpStatus.UNAUTHORIZED); }
    public static SwiftVaultException forbidden(String message)    { return new SwiftVaultException(message, HttpStatus.FORBIDDEN); }
    public static SwiftVaultException notFound(String message)     { return new SwiftVaultException(message, HttpStatus.NOT_FOUND); }
    public static SwiftVaultException conflict(String message)     { return new SwiftVaultException(message, HttpStatus.CONFLICT); }
    public static SwiftVaultException internal(String message)     { return new SwiftVaultException(message, HttpStatus.INTERNAL_SERVER_ERROR); }
}