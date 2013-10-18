package io.fathom.cloud.compute.api.aws.ec2;

import java.util.UUID;

public class AwsRequestContext {
    final String requestId;

    public AwsRequestContext() {
        this.requestId = UUID.randomUUID().toString();
    }

    public String getRequestId() {
        return requestId;
    }

}
