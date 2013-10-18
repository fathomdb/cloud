package io.fathom.auto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

public class JsonCodec {
    public static final Gson gson = buildGson();

    private static Gson buildGson() {
        Gson gson = new GsonBuilder().create();
        return gson;
    }

    /**
     * Format an object as JSON; handy for a simple toString implementation
     */
    public static String formatJson(Object o) {
        try {
            return gson.toJson(o);
        } catch (JsonParseException e) {
            throw new IllegalStateException("Error serializing value", e);
        }
    }
}
