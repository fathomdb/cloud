package io.fathom.cloud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DebugFormatter {
    final static Gson gson = new GsonBuilder().create();

    public static String format(Object o) {
        try {
            return gson.toJson(o);
        } catch (Exception e) {
            return "[Error: " + e.getMessage() + "]";
        }
    }

    public static String mask(String password) {
        if (password == null) {
            return null;
        }
        return "***";
    }

}
