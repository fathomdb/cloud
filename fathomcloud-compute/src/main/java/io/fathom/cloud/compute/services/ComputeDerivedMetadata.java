package io.fathom.cloud.compute.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.server.model.Project;

public interface ComputeDerivedMetadata {

    void instanceUpdated(Project project, InstanceData instance) throws CloudException;

}
