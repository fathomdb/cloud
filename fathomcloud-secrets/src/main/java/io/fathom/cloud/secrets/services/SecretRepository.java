package io.fathom.cloud.secrets.services;

import io.fathom.cloud.protobuf.SecretsModel.SecretRecordData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.state.NumberedItemCollection;
import io.fathom.cloud.state.RepositoryBase;
import io.fathom.cloud.state.StateStore.StateNode;

import javax.inject.Singleton;

@Singleton
public class SecretRepository extends RepositoryBase {

    public NumberedItemCollection<SecretRecordData> getSecrets(Project project) {
        StateNode secretNode = stateStore.getRoot("secrets");
        StateNode projectNode = secretNode.child(Long.toHexString(project.getId()));

        return new NumberedItemCollection<SecretRecordData>(projectNode, SecretRecordData.newBuilder(),
                SecretRecordData.getDescriptor().findFieldByNumber(SecretRecordData.ID_FIELD_NUMBER));
    }

}
