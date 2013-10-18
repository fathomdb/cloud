package io.fathom.cloud.dns.command;

import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.dns.services.DnsServiceImpl;
import io.fathom.cloud.protobuf.DnsModel.DnsSuffixData;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsTldCreateCommand extends TypedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(DnsTldCreateCommand.class);

    @Option(name = "-tld", usage = "tld", required = true)
    public String tld;

    @Inject
    DnsServiceImpl dns;

    public DnsTldCreateCommand() {
        super("dns-tld-create");
    }

    @Override
    protected DnsSuffixData run0() throws Exception {
        return dns.ensureTld(tld);
    }
}
