package org.example.mc.siteintegration.utils;

public class PlayerError extends Error {
    private String errorMessage;

    public PlayerError() {
        super();
    }

    public PlayerError(String message) {
        super(message);
        this.errorMessage = message;
    }

    public PlayerError(String message, Throwable cause) {
        super(message, cause);
        this.errorMessage = message;
    }

    public PlayerError(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return errorMessage;
    }
}
