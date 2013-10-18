package io.fathom.cloud.commands;

import io.fathom.cloud.protobuf.ProtobufYamlWriter;
import io.fathom.cloud.protobuf.mapper.MessageMapper;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;

public class YamlWriter {
    final Writer out;
    final ProtobufYamlWriter protobuf;

    public YamlWriter(Writer out) {
        this.out = out;
        this.protobuf = new ProtobufYamlWriter(out);
    }

    public void write(Object o) throws IOException {
        if (o instanceof Message) {
            Message message = (Message) o;
            Descriptor descriptor = message.getDescriptorForType();
            MessageMapper mapper = MessageMapper.getMessageMapper(descriptor);
            mapper.write(message, protobuf);
        } else if (o instanceof List) {
            protobuf.beginArray();
            for (Object i : (List) o) {
                write(i);
            }
            protobuf.endArray();
        } else if (o instanceof JsonElement) {
            // JsonElement json = (JsonElement) o;
            if (o instanceof JsonObject) {
                JsonObject json = (JsonObject) o;
                protobuf.beginObject();
                for (Entry<String, JsonElement> entry : json.entrySet()) {
                    protobuf.name(entry.getKey());
                    write(entry.getValue());
                }
                protobuf.endObject();
            } else if (o instanceof JsonPrimitive) {
                JsonPrimitive json = (JsonPrimitive) o;
                if (json.isBoolean()) {
                    protobuf.value(json.getAsBoolean());
                } else if (json.isString()) {
                    protobuf.value(json.getAsString());
                } else if (json.isNumber()) {
                    Number number = json.getAsNumber();
                    if (number instanceof Double || number instanceof Float) {
                        protobuf.value(number.doubleValue());
                    } else {
                        protobuf.value(number.longValue());
                    }
                } else {
                    throw new UnsupportedOperationException("Unhandled json value: " + json.toString());
                }
            } else if (o instanceof JsonArray) {
                JsonArray json = (JsonArray) o;
                protobuf.beginArray();
                for (int i = 0; i < json.size(); i++) {
                    JsonElement jsonElement = json.get(i);
                    write(jsonElement);
                }
                protobuf.endArray();
            } else {
                throw new UnsupportedOperationException("Unhandled json type: " + o.toString());
                // protobuf.value(o.toString());
            }
        } else {
            Gson gson = new Gson();
            JsonElement jsonTree = gson.toJsonTree(o);
            write(jsonTree);
            // protobuf.value(o.toString());
        }

    }
}
