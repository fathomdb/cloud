package io.fathom.cloud.server;

import io.fathom.cloud.json.GsonFieldNamingStrategy;
import io.fathom.cloud.openstack.client.DateTypeAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.google.common.base.Charsets;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.bind.NullableReflectiveTypeAdapterFactory;

@Provider
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class GsonMessageBodyReaderWriter implements MessageBodyWriter<Object>, MessageBodyReader<Object> {
    private Gson gson;

    private Gson getGson() {
        if (gson == null) {
            FieldNamingStrategy fieldNamingStrategy = new GsonFieldNamingStrategy();

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setFieldNamingStrategy(fieldNamingStrategy);

            gsonBuilder.registerTypeAdapter(Date.class, new DateTypeAdapter());

            Excluder excluder = Excluder.DEFAULT;
            Map<Type, InstanceCreator<?>> instanceCreators = Collections.emptyMap();

            ConstructorConstructor constructorConstructor = new ConstructorConstructor(instanceCreators);
            gsonBuilder.registerTypeAdapterFactory(new NullableReflectiveTypeAdapterFactory(constructorConstructor,
                    fieldNamingStrategy, excluder));

            gson = gsonBuilder.create();
        }
        return gson;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, java.lang.annotation.Annotation[] annotations,
            MediaType mediaType) {
        if (StreamingOutput.class.isAssignableFrom(type)) {
            return false;
        }

        return true;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        InputStreamReader streamReader = new InputStreamReader(entityStream, Charsets.UTF_8);
        try {
            Type jsonType;
            if (type.equals(genericType)) {
                jsonType = type;
            } else {
                jsonType = genericType;
            }

            Gson gson = getGson();
            return gson.fromJson(streamReader, jsonType);
        } finally {
            streamReader.close();
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (StreamingOutput.class.isAssignableFrom(type)) {
            return false;
        }

        return true;
    }

    @Override
    public long getSize(Object object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Object object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
            WebApplicationException {
        OutputStreamWriter writer = new OutputStreamWriter(entityStream, Charsets.UTF_8);
        try {
            Gson gson = getGson();
            gson.toJson(object, writer);
        } finally {
            writer.close();
        }
    }
}