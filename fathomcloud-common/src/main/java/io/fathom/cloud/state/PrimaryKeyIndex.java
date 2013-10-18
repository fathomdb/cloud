package io.fathom.cloud.state;

import io.fathom.cloud.CloudException;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessage.Builder;

public interface PrimaryKeyIndex {

    boolean allowDelete();

    <T extends GeneratedMessage> long createId(NumberedItemCollection<T> collection, Builder item)
            throws DuplicateValueException, CloudException;

}
