package io.fathom.auto.haproxy.template;

import io.fathom.auto.haproxy.model.Backend;
import io.fathom.auto.template.TemplateBase;
import io.fathom.cloud.openstack.client.loadbalance.model.LbaasMapping;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;

import com.google.common.base.Strings;

public class BackendTemplate extends TemplateBase<Backend> {
    static String resource = loadTemplate(BackendTemplate.class, "header");

    @Override
    public void write(Writer writer, Backend backend) throws IOException {
        init(writer);

        indent = 0;
        println("backend backend__%s", backend.key);

        indent = 1;

        println("balance roundrobin");

        boolean includeForwardedProtocol = true;

        String forwardUrl = null;
        if (backend.mappings.size() != 0) {
            LbaasMapping target0 = backend.mappings.get(0);
            forwardUrl = target0.forwardUrl;
        }

        if (!Strings.isNullOrEmpty(forwardUrl)) {
            URI uri = URI.create(forwardUrl);
            println("reqrep ^Host:.* Host:\\ %s", uri.getHost());
            println("reqrep ^([^\\ :]*)\\ /(.*) \\1\\ %s/\\2", uri.getPath());

            if (uri.getScheme().equals("https")) {
                println("server s1 %s:443 ssl", uri.getHost());
            } else {
                println("server s1 %s:80", uri.getHost());
            }

            includeForwardedProtocol = false;
        } else {
            for (LbaasMapping mapping : backend.mappings) {
                String line = "server " + mapping.key + " " + mapping.ip;
                if (mapping.port != null) {
                    line += ":" + mapping.port;
                } else {
                    line += ":" + "+8000";
                }
                println(line);
            }
        }

        if (!includeForwardedProtocol) {
            println("reqdel X-Forwarded-Protocol:\\.*");
        }
    }

}
