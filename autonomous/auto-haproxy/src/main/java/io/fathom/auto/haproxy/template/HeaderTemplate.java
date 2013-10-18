package io.fathom.auto.haproxy.template;

import io.fathom.auto.template.TemplateBase;

import java.io.IOException;
import java.io.Writer;

public class HeaderTemplate extends TemplateBase<Void> {
    static String resource = loadTemplate(HeaderTemplate.class, "header");

    @Override
    public void write(Writer writer, Void data) throws IOException {
        init(writer);
        writer.write(resource);
    }

}
