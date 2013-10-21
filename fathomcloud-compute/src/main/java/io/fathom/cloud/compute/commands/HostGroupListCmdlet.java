package io.fathom.cloud.compute.commands;

import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.compute.services.NetworkMap;
import io.fathom.cloud.protobuf.CloudModel.HostGroupData;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostGroupListCmdlet extends TypedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(HostGroupListCmdlet.class);

    @Inject
    NetworkMap networkMap;

    public HostGroupListCmdlet() {
        super("hostgroup-list");
    }

    @Override
    protected List<HostGroupData> run0() throws Exception {
        return networkMap.listHostGroups();
    }
}
