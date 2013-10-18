package io.fathom.cloud.compute.api.aws.ec2.model;

public class InstanceStateChange {
    public String instanceId;

    public InstanceState currentState;
    public InstanceState previousState;
}
