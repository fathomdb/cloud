package io.fathom.cloud.compute.api.aws.ec2.actions;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.aws.ec2.model.DescribeInstancesResponse;
import io.fathom.cloud.compute.api.aws.ec2.model.Instance;
import io.fathom.cloud.compute.api.aws.ec2.model.DescribeInstancesResponse.ReservationSetItem;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.ReservationData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
import io.fathom.cloud.server.model.Project;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@AwsAction("DescribeInstances")
public class DescribeInstances extends AwsActionHandler {
    @Override
    public Object go() throws CloudException {
        // User user = getUser();

        Project project = getProject();

        // Get instances first; so we shouldn't have any instances that refer to
        // not-yet-create reservations
        List<InstanceData> instances = instanceStateStore.getInstances(getProject().getId()).list();
        List<ReservationData> reservationInfoList = instanceStateStore.getReservations(project).list();

        DescribeInstancesResponse response = new DescribeInstancesResponse();
        response.requestId = getRequestId();

        response.reservations = Lists.newArrayList();
        Map<Long, ReservationData> reservationInfoMap = Maps.newHashMap();
        for (ReservationData reservationInfo : reservationInfoList) {
            reservationInfoMap.put(reservationInfo.getId(), reservationInfo);
        }

        Map<Long, ReservationSetItem> xmlReservations = Maps.newHashMap();
        for (InstanceData instanceInfo : instances) {
            long reservationId = instanceInfo.getReservationId();

            ReservationData reservationInfo = reservationInfoMap.get(reservationId);
            if (reservationInfo == null) {
                throw new IllegalStateException();
            }

            ReservationSetItem reservation = xmlReservations.get(reservationId);
            if (reservation == null) {
                reservation = new ReservationSetItem();
                xmlReservations.put(reservationId, reservation);
                response.reservations.add(reservation);

                reservation.reservationId = toEc2ReservationId(reservationInfo.getId());
                reservation.ownerId = toEc2Owner(reservationInfo.getProjectId());

                List<SecurityGroupData> groups = getSecurityGroups();
                reservation.groups = buildGroupsXml(groups);

                reservation.instances = Lists.newArrayList();
            }

            Instance instance = buildRunningInstanceXml(reservationInfo, instanceInfo);
            reservation.instances.add(instance);

            instance.groups = reservation.groups;
        }

        // <instanceState>[\n]"
        // <code>16</code>[\n]"
        // <name>running</name>[\n]"
        // </instanceState>[\n]"
        //
        //
        // <privateDnsName>ip-10-248-113-58.us-west-2.compute.internal</privateDnsName>[\n]"
        // <dnsName>ec2-54-218-220-237.us-west-2.compute.amazonaws.com</dnsName>[\n]"
        //
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
        // <privateIpAddress>10.248.113.58</privateIpAddress>[\n]"
        // <ipAddress>54.218.220.237</ipAddress>[\n]"
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
        // </item>[\n]"
        // </reservationSet>[\n]"

        return response;
    }

}
