package io.fathom.auto.config;

import java.io.IOException;

public interface SecretKeys {
    public interface SecretInfo {

        String getId();

        String read() throws IOException;
    }

    SecretInfo findSecret(String host);

    void refresh() throws IOException;

}
