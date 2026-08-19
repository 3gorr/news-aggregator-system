package com.github._3gorr.joboard.source;

public class SourceFetchException extends RuntimeException {
    public SourceFetchException(String message, Throwable cause) {
        super(message, cause);
    }

    public SourceFetchException(String message) {
        super(message);
    }
}
