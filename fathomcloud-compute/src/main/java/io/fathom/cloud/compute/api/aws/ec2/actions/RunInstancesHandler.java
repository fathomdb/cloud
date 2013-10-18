package io.fathom.cloud.compute.api.aws.ec2.actions;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.actions.StartInstancesAction;
import io.fathom.cloud.compute.api.aws.ec2.model.Instance;
import io.fathom.cloud.compute.api.aws.ec2.model.RunInstancesResponse;
import io.fathom.cloud.compute.scheduler.InstanceScheduler;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.ReservationData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.ImageService;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@AwsAction("RunInstances")
public class RunInstancesHandler extends AwsActionHandler {
    private static final Logger log = LoggerFactory.getLogger(RunInstancesHandler.class);
    @Inject
    InstanceScheduler scheduler;

    @Inject
    ImageService imageService;

    @Override
    public Object go() throws CloudException {
        // User user = getUser();
        Project project = getProject();

        // ImageId=ami-5168f861
        // MinCount=1
        // MaxCount=1
        // DisableApiTermination=false
        // Monitoring.Enabled=false
        // EbsOptimized=false

        StartInstancesAction action = new StartInstancesAction();

        long imageId = decodeEc2Id(get("ImageId"));

        {
            ReservationData.Builder reservation = ReservationData.newBuilder();

            ImageService.Image image = imageService.findImage(project, imageId);
            if (image == null) {
                throw new IllegalArgumentException();
            }

            reservation.setImageId(image.getId());

            action.reservationTemplate = reservation.build();
        }

        List<SecurityGroupData> securityGroups = getSecurityGroups();

        {
            InstanceData.Builder instance = InstanceData.newBuilder();

            for (SecurityGroupData securityGroup : securityGroups) {
                instance.addSecurityGroupId(securityGroup.getId());
            }
            action.instanceTemplate = instance.build();
        }

        action.maxCount = get("MaxCount", 1);
        action.minCount = get("MinCount", 1);

        // action.user = getUser();
        action.project = project;

        StartInstancesAction.Result result = action.go();

        RunInstancesResponse response = new RunInstancesResponse();
        response.requestId = getRequestId();

        response.reservationId = toEc2ReservationId(result.reservation.getId());
        response.ownerId = toEc2Owner(project.getId());

        response.groups = buildGroupsXml(securityGroups);

        response.instances = Lists.newArrayList();

        for (InstanceData instanceInfo : result.instances) {
            Instance instance = buildRunningInstanceXml(result.reservation, instanceInfo);
            response.instances.add(instance);

            instance.groups = response.groups;
        }

        // <instanceId>i-e6655ad3</instanceId>[\n]"
        // <imageId>ami-5168f861</imageId>[\n]"
        // <instanceState>[\n]"
        // <code>0</code>[\n]"
        // <name>pending</name>[\n]"
        // </instanceState>[\n]"
        // <privateDnsName/>[\n]"
        // <dnsName/>[\n]"
        // <reason/>[\n]"
        // <amiLaunchIndex>0</amiLaunchIndex>[\n]"
        // <productCodes/>[\n]"
        // <instanceType>m1.small</instanceType>[\n]"
        // <launchTime>2013-06-25T22:18:51.000Z</launchTime>[\n]"
        // <placement>[\n]"
        // <availabilityZone>us-west-2a</availabilityZone>[\n]"
        // <groupName/>[\n]"
        // <tenancy>default</tenancy>[\n]"
        // </placement>[\n]"
        // <kernelId>aki-fc37bacc</kernelId>[\n]"
        // <monitoring>[\n]"
        // <state>disabled</state>[\n]"
        // </monitoring>[\n]"
        // <groupSet>[\n]"
        // <item>[\n]"
        // <groupId>sg-44412974</groupId>[\n]"
        // <groupName>default</groupName>[\n]"
        // </item>[\n]"
        // </groupSet>[\n]"
        // <stateReason>[\n]"
        // <code>pending</code>[\n]"
        // <message>pending</message>[\n]"
        // </stateReason>[\n]"
        // <architecture>x86_64</architecture>[\n]"
        // <rootDeviceType>instance-store</rootDeviceType>[\n]"
        // <blockDeviceMapping/>[\n]"
        // <virtualizationType>paravirtual</virtualizationType>[\n]"
        // <clientToken/>[\n]"
        // <hypervisor>xen</hypervisor>[\n]"
        // <networkInterfaceSet/>[\n]"
        // <ebsOptimized>false</ebsOptimized>[\n]"
        // </item>[\n]"
        // </instancesSet>[\n]"

        return response;
    }
}
