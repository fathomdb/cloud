package io.fathom.cloud.storage.services;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MimeTypesTest {
    @Test
    public void test() {
        assertEquals("text/plain", MimeTypes.INSTANCE.guessMimeType("hello.txt"));
        assertEquals("application/msword", MimeTypes.INSTANCE.guessMimeType("hello.doc"));
        assertEquals("text/html", MimeTypes.INSTANCE.guessMimeType("hello.htm"));
        assertEquals("text/html", MimeTypes.INSTANCE.guessMimeType("hello.html"));

        assertEquals("text/plain", MimeTypes.INSTANCE.guessMimeType("/somepath/hello.txt"));

        assertEquals(null, MimeTypes.INSTANCE.guessMimeType("hello"));
        assertEquals(null, MimeTypes.INSTANCE.guessMimeType("/"));
        assertEquals(null, MimeTypes.INSTANCE.guessMimeType("somepath/"));
    }
}
