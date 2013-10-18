package io.fathom.cloud.dns.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.dns.DnsService;
import io.fathom.cloud.dns.model.DnsZone;
import io.fathom.cloud.openstack.client.dns.model.WrappedZone;
import io.fathom.cloud.openstack.client.dns.model.Zone;
import io.fathom.cloud.openstack.client.dns.model.ZoneList;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.state.DuplicateValueException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@Path("/openstack/dns/{project}/v2/zones")
public class ZoneResource extends ZonesResourceBase {
    private static final Logger log = LoggerFactory.getLogger(ZoneResource.class);

    @Inject
    DnsService dnsService;

    @GET
    public ZoneList listZones() throws CloudException {
        ZoneList zones = new ZoneList();
        zones.zones = Lists.newArrayList();

        for (DnsService.Zone domain : dnsService.listZones(getProject())) {
            zones.zones.add(toModel((DnsZone) domain));
        }
        return zones;
    }

    @POST
    public WrappedZone createZone(WrappedZone request) throws CloudException {
        Project project = getProject();

        Zone zone = request.zone;

        DnsService.DnsZoneSpec zoneSpec = new DnsService.DnsZoneSpec();
        zoneSpec.name = zone.name;

        DnsService.Zone domain;
        try {
            domain = dnsService.createZone(project, zoneSpec);
        } catch (DuplicateValueException e) {
            throw new WebApplicationException(Status.CONFLICT);
        }

        WrappedZone response = new WrappedZone();
        response.zone = toModel((DnsZone) domain);
        return response;
    }

    private Zone toModel(DnsZone zone) {
        Zone model = new Zone();
        // zone.id = domain.getData().getDomain();
        model.name = zone.getName();
        model.project_id = "" + zone.getProjectId();
        model.id = "" + zone.getId();
        return model;
    }

}