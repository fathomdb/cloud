package io.fathom.cloud.dns.api.os.resources;

import io.fathom.cloud.Clock;
import io.fathom.cloud.CloudException;
import io.fathom.cloud.dns.DnsService;
import io.fathom.cloud.dns.model.DnsRecord;
import io.fathom.cloud.dns.model.DnsRecordset;
import io.fathom.cloud.dns.model.DnsZone;
import io.fathom.cloud.dns.services.DnsServiceImpl;
import io.fathom.cloud.openstack.client.dns.model.Record;
import io.fathom.cloud.openstack.client.dns.model.Recordset;
import io.fathom.cloud.openstack.client.dns.model.RecordsetList;
import io.fathom.cloud.openstack.client.dns.model.WrappedRecordset;
import io.fathom.cloud.protobuf.DnsModel.DnsRecordData;
import io.fathom.cloud.protobuf.DnsModel.DnsRecordsetData;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@Path("/openstack/dns/{project}/v2/zones/{zoneid}/recordsets")
public class RecordsetsResource extends ZonesResourceBase {
    private static final Logger log = LoggerFactory.getLogger(RecordsetsResource.class);

    @Inject
    DnsServiceImpl dnsService;

    @PathParam("zoneid")
    Long zoneId;

    @GET
    public RecordsetList list() throws CloudException {
        return list(false);
    }

    RecordsetList list(boolean details) throws CloudException {
        RecordsetList recordsets = new RecordsetList();
        recordsets.recordsets = Lists.newArrayList();

        DnsZone domain = getZone();
        for (DnsService.Recordset record : dnsService.listRecordsets(getProject(), domain)) {
            recordsets.recordsets.add(toModel(domain, (DnsRecordset) record, true));
        }

        return recordsets;
    }

    @GET
    @Path("details")
    public RecordsetList listDetails() throws CloudException {
        return list(true);
    }

    @POST
    public WrappedRecordset createRecordset(WrappedRecordset request) throws CloudException {
        DnsZone zone = getZone();

        Recordset recordset = request.recordset;
        DnsRecordsetData.Builder b = DnsRecordsetData.newBuilder();
        b.setFqdn(recordset.name);
        b.setType(recordset.type);

        for (Record record : recordset.records) {
            DnsRecordData.Builder rb = b.addRecordBuilder();
            if (record.value != null) {
                rb.setTarget(record.value);
            }
            if (record.port != null) {
                rb.setPort(record.port);
            }
            if (record.priority != null) {
                rb.setPriority(record.priority);
            }
            if (record.weight != null) {
                rb.setWeight(record.weight);
            }
        }
        DnsRecordset created = dnsService.createRecordset(getProject(), zone, b);

        WrappedRecordset response = new WrappedRecordset();
        response.recordset = toModel(zone, created, true);
        return response;
    }

    @DELETE
    @Path("{id}")
    public void deleteRecordset(@PathParam("id") Long recordsetId) throws CloudException {
        DnsZone zone = getZone();

        dnsService.deleteRecordset(getProject(), zone, recordsetId);
    }

    @GET
    @Path("{id}")
    public WrappedRecordset read(@PathParam("id") Long recordsetId) throws CloudException {
        DnsZone zone = getZone();

        DnsService.Recordset record = dnsService.findRecordset(getProject(), zone, recordsetId);
        notFoundIfNull(record);

        WrappedRecordset response = new WrappedRecordset();
        response.recordset = toModel(zone, (DnsRecordset) record, true);

        return response;
    }

    private DnsZone getZone() throws CloudException {
        DnsZone domain = dnsService.findDomain(getProject(), zoneId);
        notFoundIfNull(domain);
        return domain;
    }

    static Recordset toModel(DnsZone domain, DnsRecordset recordset, boolean details) {
        Recordset model = new Recordset();

        // zone.id = domain.getData().getDomain();
        DnsRecordsetData data = recordset.getData();
        model.id = "" + data.getId();
        model.type = data.getType();
        model.zone_id = "" + domain.getData().getId();

        model.name = recordset.getFqdn();
        if (data.hasTtl()) {
            model.ttl = data.getTtl();
        }
        if (data.hasWeight()) {
            model.weight = data.getWeight();
        }
        model.status = "ACTIVE";
        model.version = 1L;

        if (data.hasState()) {
            model.created_at = Clock.toDate(data.getState().getCreatedAt());
            model.updated_at = Clock.toDate(data.getState().getUpdatedAt());
            model.deleted_at = Clock.toDate(data.getState().getDeletedAt());
        }

        if (details) {
            model.records = Lists.newArrayList();
            for (DnsRecord record : recordset.getRecords()) {
                model.records.add(toModel(record));
            }
        }

        return model;
    }

    static Record toModel(DnsRecord record) {
        DnsRecordData data = record.getData();

        Record model = new Record();
        if (data.hasTarget()) {
            model.value = data.getTarget();
        }
        if (data.hasWeight()) {
            model.weight = data.getWeight();
        }
        if (data.hasPort()) {
            model.port = data.getPort();
        }
        if (data.hasPriority()) {
            model.priority = data.getPriority();
        }
        return model;
    }

}