package io.fathom.cloud.compute.api.os.resources;

import com.google.common.base.Strings;

public class OpenstackIds {

    public static long toImageId(String s) {
        return getIdFromRef(s);
    }

    public static long toFlavorId(String s) {
        return getIdFromRef(s);
    }

    private static long getIdFromRef(String s) {
        if (Strings.isNullOrEmpty(s)) {
            throw new IllegalArgumentException();
        }

        int lastSlashIndex = s.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            s = s.substring(lastSlashIndex + 1);
        }

        return Long.valueOf(s);
    }

}
