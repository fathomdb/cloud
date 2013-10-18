package io.fathom.auto.template;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

public abstract class TemplateBase<T> {
    private static final Logger log = LoggerFactory.getLogger(TemplateBase.class);

    protected Writer writer;
    protected int indent;

    protected static String loadTemplate(Class<?> contextClass, String resourceName) {
        URL url = Resources.getResource(contextClass, resourceName);
        try {
            String s = Resources.toString(url, Charsets.UTF_8);
            return s;
        } catch (IOException e) {
            throw new IllegalStateException("Error reading resource: " + resourceName, e);
        }
    }

    public abstract void write(Writer writer, T data) throws IOException;

    protected void println(String fmt, Object... args) throws IOException {
        String s = String.format(fmt, args);
        println(s);
    }

    protected void init(Writer writer) {
        this.writer = writer;
        this.indent = 0;
    }

    protected void println(String s) throws IOException {
        for (int i = 0; i < indent; i++) {
            writer.write('\t');
        }

        if (s != null) {
            writer.write(s);
        }

        writer.write('\n');
    }

    public void write(File dest, T data) throws IOException {
        StringWriter writer = new StringWriter();
        write(writer, data);

        log.info("Installing new file {}", dest);

        Files.write(writer.toString(), dest, Charsets.UTF_8);
    }
}
