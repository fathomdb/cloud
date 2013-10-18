package io.fathom.cloud.mq;

import java.io.IOException;

public interface MessageQueueReader {
    byte[] poll() throws IOException;
}
