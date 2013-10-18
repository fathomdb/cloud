package io.fathom.cloud.mq;

import java.io.IOException;

public interface RequestExecutor {
    void execute(byte[] request) throws IOException;
}
