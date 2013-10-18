package io.fathom.cloud;

public class CloudException extends Exception {
    private static final long serialVersionUID = 1L;

    public CloudException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudException(String message) {
        super(message);
    }

}
