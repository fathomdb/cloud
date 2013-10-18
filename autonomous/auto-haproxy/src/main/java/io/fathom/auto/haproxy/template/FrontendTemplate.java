package io.fathom.auto.haproxy.template;

import io.fathom.auto.haproxy.model.Backend;
import io.fathom.auto.haproxy.model.Frontend;
import io.fathom.auto.template.TemplateBase;

import java.io.IOException;
import java.io.Writer;

import com.google.common.base.Strings;

public class FrontendTemplate extends TemplateBase<Frontend> {

    static String resource = loadTemplate(FrontendTemplate.class, "frontend");

    @Override
    public void write(Writer writer, Frontend frontend) throws IOException {
        init(writer);

        boolean useSsl = !Strings.isNullOrEmpty(frontend.sslKey);

        indent = 0;
        println("frontend http");

        indent = 1;
        println("bind :::80");
        if (useSsl) {
            println("bind :::443 ssl crt " + frontend.sslKey);
        }

        println("option http-server-close");

        println("acl has-x-forwarded-protocol hdr_cnt(x-forwarded-protocol) gt 0");
        println("block if has-x-forwarded-protocol");

        if (useSsl) {
            println("acl is-ssl  ssl_fc");

            println("reqadd  X-Forwarded-Protocol:\\ https if is-ssl");
            println("reqadd  X-Forwarded-Protocol:\\ http if ! is-ssl");
        } else {
            println("reqadd  X-Forwarded-Protocol:\\ http");
        }
        for (Backend backend : frontend.backends) {
            if (backend.host == null) {
                writer.write(String.format("\tdefault_backend backend__%s\n", backend.key));
            } else {
                writer.write(String.format("\tacl host__%s hdr(host) -i %s\n", backend.key, backend.host));
                writer.write(String.format("\tuse_backend backend__%s if host__%s\n", backend.key, backend.host));
            }
        }
    }
}
