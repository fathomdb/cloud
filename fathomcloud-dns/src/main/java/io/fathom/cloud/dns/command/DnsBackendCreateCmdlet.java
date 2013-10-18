package io.fathom.cloud.dns.command;

import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.dns.services.DnsBackends;
import io.fathom.cloud.protobuf.DnsModel.BackendData;
import io.fathom.cloud.protobuf.DnsModel.BackendSecretData;
import io.fathom.cloud.protobuf.DnsModel.DnsBackendProviderType;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;

import com.google.common.base.Strings;

public class DnsBackendCreateCmdlet extends TypedCmdlet {

    @Option(name = "-e", usage = "email", required = false, metaVar = "EMAIL")
    public String email;

    @Option(name = "-s", usage = "server", required = false, metaVar = "URL")
    public String server;

    @Option(name = "-key", usage = "provider key", required = true, metaVar = "ID")
    public String key;

    @Option(name = "-type", usage = "provider type", required = false)
    public DnsBackendProviderType type = DnsBackendProviderType.OPENSTACK;

    @Option(name = "-default", usage = "default", required = false)
    public boolean defaultProvider = false;

    @Option(name = "-username", usage = "Backend service username", required = false)
    public String username;

    @Option(name = "-secret", usage = "Backend service secret / password", required = false)
    public String secret;

    @Inject
    DnsBackends dnsBackends;

    public DnsBackendCreateCmdlet() {
        super("dns-backend-create");
    }

    @Override
    protected BackendData run0() throws Exception {
        if (server == null) {
            switch (type) {
            case OPENSTACK:
                this.server = "https://api-cloud.fathomdb.com/openstack/identity/";
                break;
            }
        }

        if (Strings.isNullOrEmpty(email)) {
            if (type == DnsBackendProviderType.OPENSTACK) {
                throw new IllegalArgumentException("Email is required when using remote openstack DNS");
            }
        }

        BackendData.Builder b = BackendData.newBuilder();
        b.setKey(key);
        b.setType(type);
        if (server != null) {
            b.setUrl(server);
        }
        b.setDefault(defaultProvider);

        BackendSecretData.Builder sb = BackendSecretData.newBuilder();
        {
            if (username != null) {
                sb.setUsername(username);
            }
            if (secret != null) {
                sb.setPassword(secret);
            }
        }

        BackendData registration = dnsBackends.register(b, sb, email);

        return registration;
    }

}
