package io.fathom.cloud.dns.command;

import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.dns.services.DnsServiceImpl;
import io.fathom.cloud.protobuf.DnsModel.DnsSuffixData;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsTldListCommand extends TypedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(DnsTldListCommand.class);

    @Inject
    DnsServiceImpl dns;

    public DnsTldListCommand() {
        super("dns-tld-list");
    }

    @Override
    protected List<DnsSuffixData> run0() throws Exception {
        List<DnsSuffixData> tlds = dns.listSuffix();
        return tlds;
    }

}
