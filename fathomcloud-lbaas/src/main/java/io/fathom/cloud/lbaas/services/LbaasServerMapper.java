package io.fathom.cloud.lbaas.services;

import io.fathom.cloud.openstack.client.loadbalance.model.LbaasServer;
import io.fathom.cloud.protobuf.LbaasModel.LbaasServerData;
import io.fathom.cloud.server.model.Project;

public class LbaasServerMapper extends Mapper<LbaasServerData, LbaasServer> {

    public static final LbaasServerMapper INSTANCE = new LbaasServerMapper();

    @Override
    public LbaasServerData toComparable(LbaasServerData d) {
        LbaasServerData.Builder b = LbaasServerData.newBuilder(d);
        b.clearId();
        b.clearState();
        return b.build();
    }

    @Override
    public LbaasServerData toData(Project project, String systemKey, LbaasServer model) {
        // TODO: We could probably auto-map this
        LbaasServerData.Builder b = LbaasServerData.newBuilder();

        if (model.ip != null) {
            b.setIp(model.ip);
        }

        b.setSystemKey(systemKey);
        b.setProjectId(project.getId());

        LbaasServerData record = b.build();
        return record;
    }

    @Override
    public LbaasServer toModel(LbaasServerData data) {
        LbaasServer model = new LbaasServer();

        if (data.hasIp()) {
            model.ip = data.getIp();
        }

        return model;
    }
}
