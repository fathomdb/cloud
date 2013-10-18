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

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsRecordCreateCommand extends AuthenticatedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(DnsRecordCreateCommand.class);

    @Option(name = "-name", usage = "name", required = true)
    public String fqdn;

    @Option(name = "-type", usage = "type", required = false)
    public String type = "A";

    @Option(name = "-value", usage = "value", required = true)
    public List<String> values;

    @Inject
    DnsServiceImpl dns;

    public DnsRecordCreateCommand() {
        super("dns-record-create");
    }

    @Override
    protected DnsRecordsetData run0() throws Exception {
        Auth auth = getAuth();
        Project project = auth.getProject();

        DnsZone zone = dns.findMaximalZone(project, fqdn);
        if (zone == null) {
            throw new IllegalArgumentException("Cannot find matching zone");
        }

        DnsRecordset created = dns.createRecordset(project, zone, fqdn, type, values);

        return created.getData();
    }

}
