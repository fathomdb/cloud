package io.fathom.cloud.state;

import io.fathom.cloud.state.StateStore.StateNode;

import java.util.Random;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessage.Builder;

public class RandomPrimaryKeyStrategy implements PrimaryKeyIndex {
    private final StateNode parentNode;
    private final FieldDescriptor idField;

    public RandomPrimaryKeyStrategy(StateNode parentNode, FieldDescriptor idField) {
        this.parentNode = parentNode;
        this.idField = idField;
    }

    @Override
    public <T extends GeneratedMessage> long createId(NumberedItemCollection<T> collection, Builder item)
            throws StateStoreException {
        while (true) {
            // Assign a new id, randomly
            // Note that we actually assign from the int32 range, for now...
            long id = getRandom(Integer.MAX_VALUE);
            if (parentNode.hasChild(Long.toHexString(id))) {
                continue;
            }
            return id;
        }
    }

    final Random random = new Random();

    synchronized int getRandom(int max) {
        return random.nextInt(max);
    }

    @Override
    public boolean allowDelete() {
        return true;
    }

}
