package io.fathom.cloud.blobs;

import java.util.concurrent.ConcurrentLinkedQueue;

// TODO: This is what KeyCzar does.
// However, it looks like this is prone to leaking memory?
class SimplePool<T> extends ConcurrentLinkedQueue<T> {
    private static final long serialVersionUID = 4914617278167817144L;
}