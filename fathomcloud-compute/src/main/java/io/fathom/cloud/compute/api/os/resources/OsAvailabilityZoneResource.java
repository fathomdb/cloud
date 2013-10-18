package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.AvailabilityZone;
import io.fathom.cloud.compute.api.os.model.AvailabilityZoneList;
import io.fathom.cloud.compute.api.os.model.ZoneState;
import io.fathom.cloud.server.model.Project;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Path("/openstack/compute/{project}/os-availability-zone")
public class OsAvailabilityZoneResource extends ComputeResourceBase {
    private static final Logger log = LoggerFactory.getLogger(OsAvailabilityZoneResource.class);

    @GET
    public AvailabilityZoneList list() throws CloudException {
        return list(false);
    }

    @GET
    @Path("detail")
    public AvailabilityZoneList listDetail() throws CloudException {
        return list(true);
    }

    private AvailabilityZoneList list(boolean details) throws CloudException {
        Project project = getProject();

        warnStub();

        AvailabilityZoneList response = new AvailabilityZoneList();
        response.availabilityZoneInfo = Lists.newArrayList();

        AvailabilityZone availabilityZone = new AvailabilityZone();
        availabilityZone.zoneName = "default";
        availabilityZone.zoneState = new ZoneState();
        availabilityZone.zoneState.available = true;
        availabilityZone.hosts = Maps.newHashMap();

        response.availabilityZoneInfo.add(availabilityZone);

        return response;
    }

    // private Record toModel(DnsRecord record) {
    // DnsRecordData data = record.getData();
    //
    // Record model = new Record();
    // model.domain = record.getDomain().getName();
    // model.ip = data.getIp();
    // model.type = data.getType();
    // model.name = data.getName();
    //
    // return model;
    // }

}
