package io.fathom.cloud.lbaas.state;

import io.fathom.cloud.protobuf.LbaasModel.LbaasMappingData;
import io.fathom.cloud.protobuf.LbaasModel.LbaasServerData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.state.NumberedItemCollection;
import io.fathom.cloud.state.RepositoryBase;
import io.fathom.cloud.state.StateStore.StateNode;

import javax.inject.Singleton;

@Singleton
public class LbaasRepository extends RepositoryBase {
    public NumberedItemCollection<LbaasMappingData> getMappings(Project project) {
        StateNode node = stateStore.getRoot("lb_map");
        node = node.child(Long.toHexString(project.getId()));
        return NumberedItemCollection.builder(node, LbaasMappingData.class).idField(LbaasMappingData.ID_FIELD_NUMBER)
                .create();
    }

    public NumberedItemCollection<LbaasServerData> getServers(Project project) {
        StateNode node = stateStore.getRoot("lb_server");
        node = node.child(Long.toHexString(project.getId()));
        return NumberedItemCollection.builder(node, LbaasServerData.class).idField(LbaasServerData.ID_FIELD_NUMBER)
                .create();
    }

}
