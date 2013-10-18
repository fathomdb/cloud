package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.Domain;
import io.fathom.cloud.compute.api.os.model.DomainList;
import io.fathom.cloud.compute.api.os.model.Record;
import io.fathom.cloud.compute.api.os.model.RecordList;
import io.fathom.cloud.compute.api.os.model.WrappedDomain;
import io.fathom.cloud.compute.api.os.model.WrappedRecord;
import io.fathom.cloud.dns.DnsService;
import io.fathom.cloud.dns.DnsService.DnsZoneSpec;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.state.DuplicateValueException;

import java.net.InetAddress;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;

@Path("/openstack/compute/{project}/os-floating-ip-dns")
public class OsFloatingIpDnsResource extends ComputeResourceBase {
    private static final Logger log = LoggerFactory.getLogger(OsFloatingIpDnsResource.class);

    @Inject
    DnsService dnsService;

    @GET
    public DomainList listDomains() throws CloudException {
        Project project = getProject();

        DomainList response = new DomainList();
        response.domain_entries = Lists.newArrayList();
        for (DnsService.Zone domain : dnsService.listZones(project)) {
            response.domain_entries.add(toModel(domain));
        }

        return response;
    }

    @GET
    @Path("{domain}/entries/{nameOrIp}")
    public Response listRecords(@PathParam("domain") String domainName, @PathParam("nameOrIp") String nameOrIp)
            throws CloudException {
        boolean isIp = false;

        if (nameOrIp.contains(":")) {
            // IPv6 address
            isIp = true;
        } else {
            char firstChar = nameOrIp.charAt(0);
            if (Character.isDigit(firstChar)) {
                // IPv4
                isIp = true;
            } else {
                isIp = false;
            }
        }

        if (isIp) {
            RecordList response = listRecordsByIp(domainName, nameOrIp);
            return Response.ok().entity(response).build();
        } else {
            WrappedRecord response = findRecordByFqdn(domainName, nameOrIp);
            return Response.ok().entity(response).build();
        }
    }

    @PUT
    @Path("{domain}/entries/{name}")
    public WrappedRecord createRecord(@PathParam("domain") String domainName, @PathParam("name") String name,
            WrappedRecord wrappedRequest) throws CloudException {
        Project project = getProject();

        Record request = wrappedRequest.dns_entry;
        if (request == null) {
            throw new IllegalArgumentException();
        }

        // Sanity
        if (request.type == null) {
            request.type = request.dns_type;
        }

        request.domain = domainName;
        request.name = name;

        DnsService.Zone domain = dnsService.findZoneByName(project, domainName);
        notFoundIfNull(domain);

        List<String> ips = Lists.newArrayList();
        ips.add(request.ip);

        String fqdn = name + "." + domainName;
        DnsService.Recordset recordset = dnsService.createRecordset(project, domain, fqdn, request.type, ips);
        DnsService.Record record = recordset.getRecords().get(0);

        WrappedRecord response = new WrappedRecord();
        response.dns_entry = toModel(recordset, record);
        return response;
    }

    RecordList listRecordsByIp(String domainName, String ipString) throws CloudException {
        InetAddress ip = InetAddresses.forString(ipString);

        Project project = getProject();

        DnsService.Zone domain = dnsService.findZoneByName(project, domainName);
        notFoundIfNull(domain);

        RecordList response = new RecordList();
        response.dns_entries = Lists.newArrayList();
        for (DnsService.Recordset recordset : dnsService.listRecordsets(project, domain)) {
            for (DnsService.Record record : recordset.getRecords()) {
                if (ip.equals(record.getTarget())) {
                    response.dns_entries.add(toModel(recordset, record));
                }
            }
        }

        return response;
    }

    WrappedRecord findRecordByFqdn(String domainName, String fqdn) throws CloudException {
        Project project = getProject();

        DnsService.Zone domain = dnsService.findZoneByName(project, domainName);
        notFoundIfNull(domain);

        // TODO: Return multiple matches??
        DnsService.Recordset found = null;
        for (DnsService.Recordset record : dnsService.listRecordsets(project, domain)) {
            if (fqdn.equals(record.getFqdn())) {
                found = record;
                break;
            }
        }
        notFoundIfNull(found);

        DnsService.Record record = found.getRecords().get(0);

        WrappedRecord response = new WrappedRecord();
        response.dns_entry = toModel(found, record);
        return response;
    }

    @PUT
    @Path("{name}")
    public WrappedDomain createDomain(@PathParam("name") String name, WrappedDomain domain) throws CloudException {
        Project project = getProject();

        Domain request = domain.domain_entry;
        if (request == null) {
            throw new IllegalArgumentException();
        }

        request.domain = name;

        if (!request.scope.equals("public")) {
            throw new UnsupportedOperationException();
        }
        if (request.project != null) {
            throw new UnsupportedOperationException();
        }
        if (request.availability_zone != null) {
            throw new UnsupportedOperationException();
        }

        DnsZoneSpec zoneSpec = new DnsZoneSpec();
        zoneSpec.name = request.domain;

        DnsService.Zone created;
        try {
            created = dnsService.createZone(project, zoneSpec);
        } catch (DuplicateValueException e) {
            throw new WebApplicationException(Status.CONFLICT);
        }

        WrappedDomain response = new WrappedDomain();
        response.domain_entry = toModel(created);
        return response;
    }

    private Domain toModel(DnsService.Zone domain) {
        Domain model = new Domain();
        model.domain = domain.getName();
        model.scope = "public";
        return model;
    }

    // private Record toModel(DnsService.Recordset recordset) {
    // Record model = new Record();
    // model.domain = recordset.getDomain().getName();
    //
    // DnsService.Record found = null;
    // for (DnsService.Record r : recordset.getRecords()) {
    // found = r;
    // }
    // if (found == null) {
    // throw new IllegalStateException();
    // }
    // model.ip = found.getAddress();
    //
    // model.type = recordset.getType();
    // model.name = recordset.getName();
    //
    // return model;
    // }

    private Record toModel(DnsService.Recordset recordset, DnsService.Record record) {
        Record model = new Record();
        model.domain = recordset.getZone().getName();

        model.ip = record.getTarget();

        model.type = recordset.getType();
        model.name = recordset.getFqdn();

        return model;
    }
}
