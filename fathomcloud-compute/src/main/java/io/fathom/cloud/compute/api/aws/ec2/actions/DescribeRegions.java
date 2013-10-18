package io.fathom.cloud.compute.api.aws.ec2.actions;

import io.fathom.cloud.compute.api.aws.ec2.model.DescribeRegionsResponse;

import com.google.common.collect.Lists;

@AwsAction("DescribeRegions")
public class DescribeRegions extends AwsActionHandler {
    @Override
    public Object go() {
        DescribeRegionsResponse response = new DescribeRegionsResponse();
        response.requestId = getRequestId();
        response.regionInfo = new DescribeRegionsResponse.RegionInfo();
        response.regionInfo.regions = Lists.newArrayList();
        DescribeRegionsResponse.RegionInfo.Item region = new DescribeRegionsResponse.RegionInfo.Item();
        region.regionName = "primary";
        region.regionEndpoint = "127.0.0.1:8088";
        response.regionInfo.regions.add(region);

        return response;
    }
}
