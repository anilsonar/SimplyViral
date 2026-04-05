package com.simplyviral.shared.exception;

public class SimplyViralException extends RuntimeException {
    public SimplyViralException(String message) {
        super(message);
    }

    public SimplyViralException(String message, Throwable cause) {
        super(message, cause);
    }
}
