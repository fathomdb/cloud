package io.fathom.cloud.dns.command;

import io.fathom.cloud.commands.AuthenticatedCmdlet;
import io.fathom.cloud.dns.model.DnsRecordset;
import io.fathom.cloud.dns.model.DnsZone;
import io.fathom.cloud.dns.services.DnsServiceImpl;
import io.fathom.cloud.protobuf.DnsModel.DnsRecordsetData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsRecordListCommand extends AuthenticatedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(DnsRecordListCommand.class);

    @Option(name = "-zone", usage = "zone", required = true)
    public String zoneName;

    @Inject
    DnsServiceImpl dns;

    public DnsRecordListCommand() {
        super("dns-record-list");
    }

    @Override
    protected List<DnsRecordsetData> run0() throws Exception {
        Auth auth = getAuth();
        Project project = auth.getProject();

        DnsZone zone = dns.findZoneByName(project, zoneName);
        if (zone == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        List<DnsRecordset> records = dns.listRecordsets(project, zone);
        return DnsRecordset.toData(records);
    }

}
