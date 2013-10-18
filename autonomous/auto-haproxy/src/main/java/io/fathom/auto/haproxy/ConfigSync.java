package io.fathom.auto.haproxy;

import java.io.IOException;

public interface ConfigSync {

    int firstSync() throws IOException;

    int refresh() throws IOException;

    boolean isDirty();

    void markClean();

}
