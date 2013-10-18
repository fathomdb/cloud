package io.fathom.cloud.protobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.google.common.base.Charsets;
import com.google.gson.stream.JsonReader;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;

@Provider
@Singleton
@Consumes(MediaType.APPLICATION_JSON)
public final class ProtobufMessageBodyReader implements MessageBodyReader<Object> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (MessageOrBuilder.class.isAssignableFrom(type)) {
            return true;
        }
        return false;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException,
            WebApplicationException {

        try (InputStreamReader streamReader = new InputStreamReader(entityStream, Charsets.UTF_8)) {
            try (JsonReader jsonReader = new JsonReader(streamReader)) {
                Message.Builder builder;

                boolean returnBuilder;

                if (Message.Builder.class.isAssignableFrom(type)) {
                    builder = ProtobufUtils.newBuilder(type);
                    returnBuilder = true;
                } else if (Message.class.isAssignableFrom(type)) {
                    builder = ProtobufUtils.newBuilder(type);
                    returnBuilder = false;
                } else {
                    throw new UnsupportedOperationException();
                }

                ProtobufJsonReader.deserialize(builder, jsonReader);

                if (returnBuilder) {
                    return builder;
                } else {
                    return builder.build();
                }
            }
        }
    }
}