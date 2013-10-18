package io.fathom.cloud.compute.api.aws.ec2.actions;

import io.fathom.cloud.compute.api.aws.ec2.model.DeleteKeyPairResponse;

@AwsAction("DeleteKeyPair")
public class DeleteKeyPair extends AwsActionHandler {
    @Override
    public Object go() {
        DeleteKeyPairResponse response = new DeleteKeyPairResponse();
        response.requestId = getRequestId();
        response.returnValue = true;

        return response;
    }
}
