package io.fathom.cloud.mq;

import java.io.IOException;

public interface MessageQueueWriter {
    void enqueue(byte[] data) throws IOException;
}
