package io.fathom.cloud.state;

import com.google.common.util.concurrent.ListenableFuture;

public class Watched<T> {

    final T value;
    final ListenableFuture<?> future;

    public Watched(T value, ListenableFuture<?> future) {
        this.value = value;
        this.future = future;
    }

    public T getValue() {
        return value;
    }

    public ListenableFuture<?> getFuture() {
        return future;
    }

}
