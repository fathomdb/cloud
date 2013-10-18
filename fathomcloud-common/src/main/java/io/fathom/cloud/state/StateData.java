package io.fathom.cloud.state;

import com.google.protobuf.ByteString;

public class StateData {
    final ByteString data;
    final long version;

    public StateData(ByteString data, long version) {
        super();
        this.data = data;
        this.version = version;
    }

}
