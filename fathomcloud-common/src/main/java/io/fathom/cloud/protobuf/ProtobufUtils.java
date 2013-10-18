package io.fathom.cloud.protobuf;

import java.lang.reflect.Method;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;

public class ProtobufUtils {
    static final Map<Class<?>, Builder> builders = Maps.newHashMap();

    public static Builder newBuilder(Class<?> type) {
        Builder builder = builders.get(type);
        if (builder == null) {
            try {
                Class<?> messageClass;
                if (Message.Builder.class.isAssignableFrom(type)) {
                    messageClass = type.getEnclosingClass();
                    builder = newBuilder(messageClass);
                } else if (Message.class.isAssignableFrom(type)) {
                    Method method = type.getMethod("newBuilder");
                    builder = (Builder) method.invoke(null);
                } else {
                    throw new UnsupportedOperationException();
                }

                builders.put(type, builder);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Error building instance", e);
            }
        }
        return builder.clone();
    }
}
