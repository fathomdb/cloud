package io.fathom.cloud.server.model;

public class User {

    private final long userId;

    public User(long userId) {
        this.userId = userId;
    }

    public long getId() {
        return userId;
    }

}
