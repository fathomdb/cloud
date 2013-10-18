package io.fathom.cloud.dns.command;

import io.fathom.cloud.commands.AuthenticatedCmdlet;
import io.fathom.cloud.dns.model.DnsZone;
import io.fathom.cloud.dns.services.DnsServiceImpl;
import io.fathom.cloud.protobuf.DnsModel.DnsZoneData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsZoneListCommand extends AuthenticatedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(DnsZoneListCommand.class);

    @Inject
    DnsServiceImpl dns;

    public DnsZoneListCommand() {
        super("dns-zone-list");
    }

    @Override
    protected List<DnsZoneData> run0() throws Exception {
        Auth auth = getAuth();
        Project project = auth.getProject();

        List<DnsZone> zones = dns.listZones(project);
        return DnsZone.toData(zones);
    }

}
