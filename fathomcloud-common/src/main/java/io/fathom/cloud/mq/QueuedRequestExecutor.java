package io.fathom.cloud.mq;

import java.io.IOException;

public class QueuedRequestExecutor implements RequestExecutor {

    final MessageQueueWriter mq;

    public QueuedRequestExecutor(MessageQueueWriter mq) {
        super();
        this.mq = mq;
    }

    @Override
    public void execute(byte[] bytes) throws IOException {
        mq.enqueue(bytes);
    }

}
