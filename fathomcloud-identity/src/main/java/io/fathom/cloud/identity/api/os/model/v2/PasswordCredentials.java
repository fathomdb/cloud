package io.fathom.cloud.identity.api.os.model.v2;

import io.fathom.cloud.DebugFormatter;

public class PasswordCredentials {
    public String username;
    public String password;

    @Override
    public String toString() {
        return "PasswordCredentials [username=" + username + ", password=" + DebugFormatter.mask(password) + "]";
    }

}
