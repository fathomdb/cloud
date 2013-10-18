package io.fathom.cloud.protobuf;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.google.common.base.Charsets;
import com.google.gson.stream.JsonWriter;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;

@Provider
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public final class ProtobufMessageBodyWriter implements MessageBodyWriter<Object> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (MessageOrBuilder.class.isAssignableFrom(type)) {
            return true;
        }
        if (Iterable.class.isAssignableFrom(type)) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments.length == 1) {
                    Type actualType = actualTypeArguments[0];
                    if (actualType instanceof Class) {
                        Class actualClass = (Class) actualType;
                        if (MessageOrBuilder.class.isAssignableFrom(actualClass)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public long getSize(Object object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Object object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
            WebApplicationException {

        try (OutputStreamWriter writer = new OutputStreamWriter(entityStream, Charsets.UTF_8)) {
            try (JsonWriter jsonWriter = new JsonWriter(new BufferedWriter(writer))) {
                if (object instanceof GeneratedMessage) {
                    ProtobufJsonWriter.serialize((GeneratedMessage) object, jsonWriter);
                } else {
                    Iterable<? extends Message> iterable = (Iterable<? extends Message>) object;
                    jsonWriter.beginArray();
                    for (Message msg : iterable) {
                        ProtobufJsonWriter.serialize(msg, jsonWriter);
                    }
                    jsonWriter.endArray();
                }
            }
        }
    }
}