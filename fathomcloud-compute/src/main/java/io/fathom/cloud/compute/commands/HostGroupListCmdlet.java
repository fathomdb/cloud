package io.fathom.cloud.compute.commands;

import io.fathom.cloud.compute.services.NetworkMap;
import io.fathom.cloud.protobuf.CloudModel.HostGroupData;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostGroupListCmdlet extends NetworkMapCmdlet {
    private static final Logger log = LoggerFactory.getLogger(HostGroupListCmdlet.class);

    @Inject
    NetworkMap networkMap;

    public HostGroupListCmdlet() {
        super("hostgroup-list");
    }

    @Override
    protected HostGroupData run0() throws Exception {
        for (HostGroupData host : networkMap.listHostGroups()) {
            println(host.toString());
        }
        return null;
    }
}
