package io.fathom.cloud.dns.command;

import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.dns.services.DnsBackends;
import io.fathom.cloud.protobuf.DnsModel.BackendData;

import java.util.List;

import javax.inject.Inject;

public class DnsBackendListCmdlet extends TypedCmdlet {
    @Inject
    DnsBackends dnsBackends;

    public DnsBackendListCmdlet() {
        super("dns-backend-list");
    }

    @Override
    protected List<BackendData> run0() throws Exception {
        List<BackendData> backends = dnsBackends.listBackends();

        return backends;
    }

}
