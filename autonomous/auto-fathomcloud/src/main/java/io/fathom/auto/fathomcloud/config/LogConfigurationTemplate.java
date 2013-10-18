package io.fathom.auto.fathomcloud.config;

import io.fathom.auto.fathomcloud.FathomCloudConfig;
import io.fathom.auto.template.TemplateBase;

import java.io.IOException;
import java.io.Writer;

public class LogConfigurationTemplate extends TemplateBase<FathomCloudConfig> {
    static String resource = loadTemplate(LogConfigurationTemplate.class, "logback.xml");

    @Override
    public synchronized void write(Writer writer, FathomCloudConfig data) throws IOException {
        init(writer);

        writer.write(resource);

        println("");
        println("");
    }

}
