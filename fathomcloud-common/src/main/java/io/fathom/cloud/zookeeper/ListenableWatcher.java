package io.fathom.cloud.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.SettableFuture;

public class ListenableWatcher implements Watcher {
    private static final Logger log = LoggerFactory.getLogger(ListenableWatcher.class);

    final SettableFuture<Object> future;

    public ListenableWatcher(SettableFuture<Object> future) {
        this.future = future;
    }

    @Override
    public void process(WatchedEvent event) {
        synchronized (future) {
            if (future.isDone()) {
                log.info("ZK event after already fired: {}", event);
                return;
            }

            switch (event.getType()) {
            case NodeDataChanged:
                break;
            default:
                log.warn("Unexpected ZK event type: {}", event);
                break;
            }

            future.set(event);
        }
    }
}
