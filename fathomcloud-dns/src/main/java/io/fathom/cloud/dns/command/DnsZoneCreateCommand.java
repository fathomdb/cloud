package io.fathom.cloud.dns.command;

import io.fathom.cloud.commands.AuthenticatedCmdlet;
import io.fathom.cloud.commands.CmdletException;
import io.fathom.cloud.dns.DnsService.DnsZoneSpec;
import io.fathom.cloud.dns.model.DnsZone;
import io.fathom.cloud.dns.services.DnsServiceImpl;
import io.fathom.cloud.protobuf.DnsModel.DnsZoneData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.state.DuplicateValueException;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsZoneCreateCommand extends AuthenticatedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(DnsZoneCreateCommand.class);

    @Option(name = "-zone", usage = "zone", required = true)
    public String zone;

    @Option(name = "-shared", usage = "shared", required = false)
    public boolean shared;

    @Option(name = "-backend", usage = "backend", required = false)
    public String backend;

    @Inject
    DnsServiceImpl dns;

    public DnsZoneCreateCommand() {
        super("dns-zone-create");
    }

    @Override
    protected DnsZoneData run0() throws Exception {
        Auth auth = getAuth();
        Project project = auth.getProject();

        DnsZoneSpec zoneSpec = new DnsZoneSpec();
        zoneSpec.name = zone;
        zoneSpec.backend = backend;

        DnsZone created;

        try {
            created = dns.createZone(project, zoneSpec);
        } catch (DuplicateValueException e) {
            throw new CmdletException("Zone already exists (locally or in backend provider).");
        }

        if (shared) {
            dns.createShared(created);
        }
        return created.getData();
    }

}
