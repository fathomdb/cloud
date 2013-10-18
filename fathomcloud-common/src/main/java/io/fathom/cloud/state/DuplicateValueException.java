package io.fathom.cloud.state;

public class DuplicateValueException extends StateStoreException {
    private static final long serialVersionUID = 1L;

    public DuplicateValueException() {
        super(null);
    }

}
