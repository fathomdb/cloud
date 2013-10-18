package io.fathom.cloud.state;

import io.fathom.cloud.CloudException;

public class StateStoreException extends CloudException {

    private static final long serialVersionUID = 1L;

    public StateStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public StateStoreException(String message) {
        super(message);
    }

}
