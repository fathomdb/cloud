package io.fathom.auto.fathomcloud.config;

import io.fathom.auto.fathomcloud.FathomCloudConfig;
import io.fathom.auto.template.TemplateBase;

import java.io.IOException;
import java.io.Writer;

public class ConfigurationFileTemplate extends TemplateBase<FathomCloudConfig> {
    static String resource = loadTemplate(ConfigurationFileTemplate.class, "configuration.properties");

    @Override
    public synchronized void write(Writer writer, FathomCloudConfig data) throws IOException {
        init(writer);

        writer.write(resource);

        println("");
        println("");
    }

}
