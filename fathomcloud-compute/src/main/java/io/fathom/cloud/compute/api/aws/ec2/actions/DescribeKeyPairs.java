package io.fathom.cloud.compute.api.aws.ec2.actions;

import io.fathom.cloud.compute.api.aws.ec2.model.DescribeKeyPairsResponse;
import io.fathom.cloud.compute.api.aws.ec2.model.DescribeKeyPairsResponse.Item;

import com.google.common.collect.Lists;

@AwsAction("DescribeKeyPairs")
public class DescribeKeyPairs extends AwsActionHandler {
    @Override
    public Object go() {
        DescribeKeyPairsResponse response = new DescribeKeyPairsResponse();
        response.requestId = getRequestId();
        response.keys = Lists.newArrayList();
        Item key = new Item();
        key.keyName = "testKey";
        key.keyFingerprint = "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00";
        response.keys.add(key);

        return response;
    }
}
