package io.fathom.cloud.openstack.client;

public class RestClientException extends Exception {

    private static final long serialVersionUID = 1L;

    Integer statusCode;

    public Integer getStatusCode() {
        return statusCode;
    }

    public RestClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public RestClientException(String message) {
        super(message);
    }

    public boolean is(int code) {
        if (statusCode == null) {
            return false;
        }
        return code == statusCode.intValue();
    }

}
