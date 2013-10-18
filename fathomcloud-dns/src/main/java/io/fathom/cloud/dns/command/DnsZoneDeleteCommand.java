package io.fathom.cloud.dns.command;

import io.fathom.cloud.commands.AuthenticatedCmdlet;
import io.fathom.cloud.dns.model.DnsZone;
import io.fathom.cloud.dns.services.DnsServiceImpl;
import io.fathom.cloud.protobuf.DnsModel.DnsZoneData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsZoneDeleteCommand extends AuthenticatedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(DnsZoneDeleteCommand.class);

    @Inject
    DnsServiceImpl dns;

    @Option(name = "-zone", usage = "zone", required = true)
    public String zone;

    public DnsZoneDeleteCommand() {
        super("dns-zone-delete");
    }

    @Override
    protected DnsZoneData run0() throws Exception {
        Auth auth = getAuth();
        Project project = auth.getProject();

        DnsZone found = dns.findZoneByName(project, zone);
        if (found == null) {
            throw new IllegalArgumentException("Zone not found: " + zone);
        }
        dns.deleteZone(project, found);
        return found.getData();
    }

}
