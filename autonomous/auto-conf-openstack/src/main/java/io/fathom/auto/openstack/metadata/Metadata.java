package io.fathom.auto.openstack.metadata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Metadata {

    private final JsonObject data;

    public Metadata(JsonObject data) {
        this.data = data;
    }

    public String findTopLevel(String key) {
        JsonElement element = data.get(key);
        if (element == null) {
            return null;
        }
        return element.getAsString();
    }

    public String getTopLevel(String key) {
        String value = findTopLevel(key);
        if (value == null) {
            throw new IllegalArgumentException("Top-level key not found: " + key);
        }
        return value;
    }

    public String findMeta(String key) {
        JsonElement meta = data.get("meta");
        if (meta == null || !(meta instanceof JsonObject)) {
            return null;
        }

        JsonElement element = ((JsonObject) meta).get(key);
        if (element == null) {
            return null;
        }
        return element.getAsString();
    }

    public String getMeta(String key) {
        String value = findMeta(key);
        if (value == null) {
            throw new IllegalArgumentException("Metadata key not found: " + key);
        }
        return value;
    }

}
