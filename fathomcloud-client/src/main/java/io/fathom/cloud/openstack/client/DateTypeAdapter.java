package io.fathom.cloud.openstack.client;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
    private final DateFormat dateFormat;
    private final DateFormat dateFormatZulu;
    private final DateFormat dateFormatZuluNoMillis;

    public DateTypeAdapter() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        dateFormatZulu = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormatZulu.setTimeZone(TimeZone.getTimeZone("UTC"));

        dateFormatZuluNoMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormatZuluNoMillis.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public synchronized JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext) {
        synchronized (dateFormat) {
            String dateFormatAsString = dateFormat.format(date);
            return new JsonPrimitive(dateFormatAsString);
        }
    }

    @Override
    public synchronized Date deserialize(JsonElement jsonElement, Type type,
            JsonDeserializationContext jsonDeserializationContext) {
        DateFormat useDateFormat;

        String s = jsonElement.getAsString();
        boolean hasMillis = s.contains(".");
        if (s.endsWith("Z")) {
            useDateFormat = hasMillis ? dateFormatZulu : dateFormatZuluNoMillis;
        } else {
            if (!hasMillis) {
                throw new UnsupportedOperationException();
            }
            useDateFormat = dateFormat;
        }
        try {
            synchronized (useDateFormat) {
                return useDateFormat.parse(s);
            }
        } catch (ParseException e) {
            throw new JsonSyntaxException(s, e);
        }
    }
}