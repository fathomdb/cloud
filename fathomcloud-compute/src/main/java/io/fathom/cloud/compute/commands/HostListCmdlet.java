package io.fathom.cloud.compute.commands;

import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.compute.services.NetworkMap;
import io.fathom.cloud.protobuf.CloudModel.HostData;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostListCmdlet extends TypedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(HostListCmdlet.class);

    @Inject
    NetworkMap networkMap;

    public HostListCmdlet() {
        super("host-list");
    }

    @Override
    protected List<HostData> run0() throws Exception {
        return networkMap.listHosts();
    }

}
