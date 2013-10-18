package io.fathom.cloud.compute.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.server.model.Project;

import javax.inject.Singleton;

@Singleton
public class NullComputeDerivedMetadata implements ComputeDerivedMetadata {

    @Override
    public void instanceUpdated(Project project, InstanceData instance) throws CloudException {

    }

}
