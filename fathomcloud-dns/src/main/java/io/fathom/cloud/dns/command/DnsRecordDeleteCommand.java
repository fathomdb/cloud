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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class DnsRecordDeleteCommand extends AuthenticatedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(DnsRecordDeleteCommand.class);

    @Option(name = "-name", usage = "name", required = true)
    public String fqdn;
    @Option(name = "-id", usage = "id", required = false)
    public String id;

    @Inject
    DnsServiceImpl dns;

    public DnsRecordDeleteCommand() {
        super("dns-record-delete");
    }

    @Override
    protected DnsRecordsetData run0() throws Exception {
        Auth auth = getAuth();
        Project project = auth.getProject();

        DnsZone zone = dns.findMaximalZone(project, fqdn);
        if (zone == null) {
            throw new IllegalArgumentException("Cannot find matching zone");
        }

        List<DnsRecordsetData> matches = Lists.newArrayList();
        for (DnsRecordset recordset : dns.listRecordsets(project, zone)) {
            DnsRecordsetData data = recordset.getData();
            if (!fqdn.equals(data.getFqdn())) {
                continue;
            }
            if (id != null) {
                String idString = "" + data.getId();
                if (!idString.equals(id)) {
                    continue;
                }
            }
            matches.add(data);
        }

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No matching record found");
        }

        if (matches.size() > 1) {
            for (DnsRecordsetData match : matches) {
                println("\t" + match.getId() + "\t" + match.getFqdn());
            }

            throw new IllegalArgumentException("Multiple matching records found");
        }

        DnsRecordsetData data = Iterables.getOnlyElement(matches);

        dns.deleteRecordset(project, zone, data.getId());

        return data;
    }
}
