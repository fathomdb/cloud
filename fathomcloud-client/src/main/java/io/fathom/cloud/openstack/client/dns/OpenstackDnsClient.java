package io.fathom.cloud.openstack.client.dns;

import io.fathom.cloud.openstack.client.OpenstackServiceClientBase;
import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.dns.model.Recordset;
import io.fathom.cloud.openstack.client.dns.model.RecordsetList;
import io.fathom.cloud.openstack.client.dns.model.WrappedRecordset;
import io.fathom.cloud.openstack.client.dns.model.WrappedZone;
import io.fathom.cloud.openstack.client.dns.model.Zone;
import io.fathom.cloud.openstack.client.dns.model.ZoneList;
import io.fathom.cloud.openstack.client.identity.TokenProvider;
import io.fathom.cloud.openstack.client.storage.OpenstackStorageClient;
import io.fathom.http.HttpClient;
import io.fathom.http.HttpRequest;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenstackDnsClient extends OpenstackServiceClientBase {
    private static final Logger log = LoggerFactory.getLogger(OpenstackStorageClient.class);

    public static final String SERVICE_TYPE = "dns";

    public OpenstackDnsClient(HttpClient httpClient, URI uri, TokenProvider tokenProvider) {
        super(httpClient, uri, tokenProvider);
    }

    public List<Zone> listZones() throws RestClientException {
        HttpRequest request = buildGet("v2/zones");
        ZoneList zones = doRequest(request, ZoneList.class);
        return zones.zones;
    }

    public Zone createZone(Zone zone) throws RestClientException {
        WrappedZone request = new WrappedZone();
        request.zone = zone;

        WrappedZone response = doPost("v2/zones", request, WrappedZone.class);
        return response.zone;
    }

    public List<Recordset> listRecordsets(String zoneId, boolean details) throws RestClientException {
        String url = "v2/zones/" + zoneId + "/recordsets";
        if (details) {
            url += "/details";
        }
        HttpRequest request = buildGet(url);
        RecordsetList recordsets = doRequest(request, RecordsetList.class);
        return recordsets.recordsets;
    }

    public void deleteRecordset(String zoneId, String recordsetId) throws RestClientException {
        if (zoneId == null || zoneId.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (recordsetId == null || recordsetId.isEmpty()) {
            throw new IllegalArgumentException();
        }
        HttpRequest request = buildDelete("v2/zones/" + zoneId + "/recordsets/" + recordsetId);
        doStringRequest(request);
    }

    public Recordset createRecordset(String zoneId, Recordset recordset) throws RestClientException {
        String url = "v2/zones/" + zoneId + "/recordsets";

        WrappedRecordset data = new WrappedRecordset();
        data.recordset = recordset;

        WrappedRecordset response = doPost(url, data, WrappedRecordset.class);
        return response.recordset;
    }
}