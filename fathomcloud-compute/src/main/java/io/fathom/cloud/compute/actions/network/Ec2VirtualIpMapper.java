package io.fathom.cloud.compute.actions.network;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.networks.VirtualIp;
import io.fathom.cloud.compute.scheduler.SchedulerHost;
import io.fathom.cloud.compute.services.DatacenterManager;
import io.fathom.cloud.compute.services.Ec2DatacenterManager;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AssignPrivateIpAddressesRequest;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AssociateAddressResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceAssociation;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceAttachment;
import com.amazonaws.services.ec2.model.InstancePrivateIpAddress;
import com.amazonaws.services.ec2.model.NetworkInterface;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.collect.Lists;

public class Ec2VirtualIpMapper extends VirtualIpMapper {

    private static final Logger log = LoggerFactory.getLogger(Ec2VirtualIpMapper.class);

    // void go() {
    // AmazonEC2Client ec2;
    //
    // {
    // DescribeAddressesRequest request = new DescribeAddressesRequest();
    // DescribeAddressesResult response = ec2.describeAddresses(request);
    // for (Address address : response.getAddresses()) {
    // if ("vpc".equalsIgnoreCase(address.getDomain())) {
    // } else if ("standard".equalsIgnoreCase(address.getDomain())) {
    // } else {
    // log.warn("Unknown address type: {}", address);
    // }
    // }
    // }
    //
    // {
    // AllocateAddressRequest request = new AllocateAddressRequest();
    // request.setDomain(DomainType.Vpc);
    // AllocateAddressResult response = ec2.allocateAddress(request);
    // String publicIp = response.getPublicIp();
    // }
    //

    //
    // }

    @Override
    public String mapIp(SchedulerHost host, InstanceData guest, VirtualIp vip) throws CloudException {
        DatacenterManager datacenterManager = host.getDatacenterManager();
        if (!(datacenterManager instanceof Ec2DatacenterManager)) {
            throw new IllegalStateException("Expected EC2 datacenter manager, found: " + datacenterManager);
        }

        Ec2DatacenterManager manager = (Ec2DatacenterManager) datacenterManager;

        AmazonEC2Client ec2 = manager.getEc2Client(host);

        String ec2InstanceId = manager.findHost(host);
        if (ec2InstanceId == null) {
            throw new IllegalStateException("Unable to find EC2 instance for host: " + host);
        }

        Instance ec2Instance = describeInstance(ec2, ec2InstanceId);

        String subnetId = ec2Instance.getSubnetId();

        // Use the default network interface
        int networkInterfaceIndex = 0;

        InstanceNetworkInterface networkInterface = findNetworkInterface(ec2Instance, networkInterfaceIndex);

        if (networkInterface == null) {
            throw new UnsupportedOperationException();

            /*
             * // TODO: Reuse unattached network interfaces (with the
             * fathomcloud // tag)???
             * 
             * NetworkInterface created; { CreateNetworkInterfaceRequest request
             * = new CreateNetworkInterfaceRequest();
             * request.setSubnetId(subnetId);
             * 
             * CreateNetworkInterfaceResult response =
             * ec2.createNetworkInterface(request); created =
             * response.getNetworkInterface();
             * log.info("Created network interface {}",
             * created.getNetworkInterfaceId()); }
             * 
             * addTag(ec2, created, "fathomcloud", "1");
             * 
             * { AttachNetworkInterfaceRequest request = new
             * AttachNetworkInterfaceRequest(); request.setDeviceIndex(1);
             * request.setInstanceId(ec2InstanceId);
             * request.setNetworkInterfaceId(created.getNetworkInterfaceId());
             * 
             * AttachNetworkInterfaceResult response =
             * ec2.attachNetworkInterface(request);
             * log.info("Attached network interface as {}",
             * response.getAttachmentId()); }
             * 
             * ec2Instance = describeInstance(ec2, ec2InstanceId);
             * 
             * networkInterface = findNetworkInterface(ec2Instance, 1);
             * 
             * if (networkInterface == null) { throw new IllegalStateException(
             * "Did not find network interface after attaching: " +
             * created.getNetworkInterfaceId()); }
             */
        }

        InstancePrivateIpAddress privateIp = findUnusedIp(networkInterface);

        if (privateIp == null) {
            // TODO: Prune private ip addresses from NICs?
            // TODO: Need to tag??

            {
                AssignPrivateIpAddressesRequest request = new AssignPrivateIpAddressesRequest();
                request.setNetworkInterfaceId(networkInterface.getNetworkInterfaceId());
                request.setSecondaryPrivateIpAddressCount(1);
                ec2.assignPrivateIpAddresses(request);
            }

            ec2Instance = describeInstance(ec2, ec2InstanceId);
            networkInterface = findNetworkInterface(ec2Instance, networkInterfaceIndex);
            privateIp = findUnusedIp(networkInterface);

            if (privateIp == null) {
                throw new IllegalStateException("Unable to find private IP address");
            }
        }

        String privateIpAddress = privateIp.getPrivateIpAddress();

        {
            AssociateAddressRequest request = new AssociateAddressRequest();
            request.setPublicIp(vip.getData().getIp());
            request.setPrivateIpAddress(privateIpAddress);
            request.setNetworkInterfaceId(networkInterface.getNetworkInterfaceId());
            request.setInstanceId(ec2InstanceId);

            AssociateAddressResult response = ec2.associateAddress(request);
            log.info("Associated public IP with assocation id: {}", response.getAssociationId());
        }

        return privateIpAddress;
    }

    private void addTag(AmazonEC2Client ec2, NetworkInterface o, String key, String value) {
        addTag(ec2, o.getNetworkInterfaceId(), key, value);
    }

    private void addTag(AmazonEC2Client ec2, String id, String key, String value) {
        Tag tag = new Tag(key, value);
        List<Tag> tags = Lists.newArrayList();
        tags.add(tag);

        CreateTagsRequest request = new CreateTagsRequest();
        request.setResources(Collections.singletonList(id));
        request.setTags(tags);

        ec2.createTags(request);

        log.info("Added tag: {}={} to {}", key, value, id);
    }

    private InstancePrivateIpAddress findUnusedIp(InstanceNetworkInterface networkInterface) {
        InstanceNetworkInterfaceAssociation unused = null;
        for (InstancePrivateIpAddress privateIpAddress : networkInterface.getPrivateIpAddresses()) {
            InstanceNetworkInterfaceAssociation association = privateIpAddress.getAssociation();
            if (association == null) {
                return privateIpAddress;
            }
        }
        return null;
    }

    private InstanceNetworkInterface findNetworkInterface(Instance ec2Instance, int deviceIndex) {
        InstanceNetworkInterface networkInterface = null;
        for (InstanceNetworkInterface i : ec2Instance.getNetworkInterfaces()) {
            InstanceNetworkInterfaceAttachment attachment = i.getAttachment();
            if (attachment == null) {
                log.error("EC2 network attachment on instance was null");
                continue;
            }

            Integer attachmentDeviceIndex = attachment.getDeviceIndex();
            if (attachmentDeviceIndex == null) {
                log.error("EC2 device index was null");
                continue;
            }

            if (attachmentDeviceIndex.intValue() == deviceIndex) {
                if (networkInterface != null) {
                    throw new IllegalStateException();
                }
                networkInterface = i;
            }
        }
        return networkInterface;
    }

    private Instance describeInstance(AmazonEC2Client ec2, String ec2InstanceId) {
        Instance ec2Instance = null;
        {
            DescribeInstancesRequest request = new DescribeInstancesRequest();
            request.setInstanceIds(Collections.singletonList(ec2InstanceId));
            DescribeInstancesResult response = ec2.describeInstances(request);

            List<Reservation> reservations = response.getReservations();
            for (Reservation reservation : reservations) {
                for (Instance i : reservation.getInstances()) {
                    if (ec2Instance != null) {
                        throw new IllegalStateException();
                    }
                    ec2Instance = i;
                }
            }

            if (ec2Instance == null) {
                throw new IllegalStateException("EC2 instance not found: " + ec2InstanceId);
            }
        }
        return ec2Instance;
    }

    @Override
    public void unmapIp(SchedulerHost host, InstanceData instance, VirtualIp vip) throws CloudException {

    }
}
