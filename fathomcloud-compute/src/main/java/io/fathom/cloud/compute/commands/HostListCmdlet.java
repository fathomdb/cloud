package io.fathom.cloud.compute.commands;

import io.fathom.cloud.protobuf.CloudModel.HostData;
import io.fathom.cloud.protobuf.CloudModel.HostGroupData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostListCmdlet extends NetworkMapCmdlet {
    private static final Logger log = LoggerFactory.getLogger(HostListCmdlet.class);

    public HostListCmdlet() {
        super("host-list");
    }

    @Override
    protected HostGroupData run0() throws Exception {
        for (HostData host : networkMap.listHosts()) {
            println(host.toString());
        }
        return null;
    }

}
