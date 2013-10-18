package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.AggregateList;
import io.fathom.cloud.server.model.Project;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@Path("/openstack/compute/{project}/os-aggregates")
public class OsAggregatesResource extends ComputeResourceBase {
    private static final Logger log = LoggerFactory.getLogger(OsAggregatesResource.class);

    @GET
    public AggregateList listAggregates() throws CloudException {
        Project project = getProject();

        warnStub();

        AggregateList response = new AggregateList();
        response.aggregates = Lists.newArrayList();
        // for (DnsDomain domain : dnsService.listDomains(project)) {
        // response.domain_entries.add(toModel(domain));
        // }

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
