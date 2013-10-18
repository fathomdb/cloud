package io.fathom.cloud.lbaas.services;

import io.fathom.cloud.openstack.client.loadbalance.model.LbaasMapping;
import io.fathom.cloud.protobuf.LbaasModel.LbaasMappingData;
import io.fathom.cloud.server.model.Project;

public class LbaasMappingMapper extends Mapper<LbaasMappingData, LbaasMapping> {

    public static final LbaasMappingMapper INSTANCE = new LbaasMappingMapper();

    @Override
    public LbaasMappingData toComparable(LbaasMappingData d) {
        LbaasMappingData.Builder b = LbaasMappingData.newBuilder(d);
        b.clearId();
        b.clearState();
        return b.build();
    }

    @Override
    public LbaasMapping toModel(LbaasMappingData data) {
        LbaasMapping model = new LbaasMapping();

        if (data.hasForwardUrl()) {
            model.forwardUrl = data.getForwardUrl();
        }

        if (data.hasIp()) {
            model.ip = data.getIp();
        }

        if (data.hasHost()) {
            model.host = data.getHost();
        }

        if (data.hasPort()) {
            model.port = data.getPort();
        }

        model.key = "id_" + data.getId();

        // model.systemKey = data.getSystemKey();

        return model;
    }

    @Override
    public LbaasMappingData toData(Project project, String systemKey, LbaasMapping mapping) {
        LbaasMappingData.Builder b = LbaasMappingData.newBuilder();

        if (mapping.forwardUrl != null) {
            b.setForwardUrl(mapping.forwardUrl);
        }

        if (mapping.host != null) {
            b.setHost(mapping.host);
        }

        if (mapping.ip != null) {
            b.setIp(mapping.ip);
        }

        if (mapping.key != null) {
            throw new UnsupportedOperationException();
        }
        if (mapping.port != null) {
            b.setPort(mapping.port);
        }

        b.setSystemKey(systemKey);
        b.setProjectId(project.getId());

        LbaasMappingData record = b.build();
        return record;
    }

}
