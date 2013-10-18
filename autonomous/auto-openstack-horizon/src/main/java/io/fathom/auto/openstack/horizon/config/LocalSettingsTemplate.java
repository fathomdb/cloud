package io.fathom.auto.openstack.horizon.config;

import io.fathom.auto.openstack.horizon.HorizonConfig;
import io.fathom.auto.template.TemplateBase;

import java.io.IOException;
import java.io.Writer;

public class LocalSettingsTemplate extends TemplateBase<HorizonConfig> {
    static String resource = loadTemplate(LocalSettingsTemplate.class, "local_settings");

    @Override
    public void write(Writer writer, HorizonConfig data) throws IOException {
        init(writer);

        writer.write(resource);

        // OPENSTACK_HOST = "127.0.0.1"
        // OPENSTACK_KEYSTONE_URL = "http://%s:5000/v2.0" % OPENSTACK_HOST

        println("");
        println("");

        String identityUrl = data.getIdentityUrl();
        println("OPENSTACK_KEYSTONE_URL = \"%s\"", identityUrl);
    }

}
