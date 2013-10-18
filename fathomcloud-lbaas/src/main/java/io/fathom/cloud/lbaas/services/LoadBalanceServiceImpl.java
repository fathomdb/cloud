package io.fathom.cloud.lbaas.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.dns.DnsService;
import io.fathom.cloud.dns.DnsService.DnsRecordSpec;
import io.fathom.cloud.dns.DnsService.DnsRecordsetSpec;
import io.fathom.cloud.lbaas.backend.LbaasBackend;
import io.fathom.cloud.lbaas.backend.LbaasBackends;
import io.fathom.cloud.lbaas.state.LbaasRepository;
import io.fathom.cloud.loadbalancer.LoadBalanceService;
import io.fathom.cloud.openstack.client.loadbalance.model.LbaasMapping;
import io.fathom.cloud.openstack.client.loadbalance.model.LbaasServer;
import io.fathom.cloud.protobuf.LbaasModel.LbaasMappingData;
import io.fathom.cloud.protobuf.LbaasModel.LbaasServerData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.state.NumberedItemCollection;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.persist.Transactional;

@Singleton
@Transactional
public class LoadBalanceServiceImpl implements LoadBalanceService {
    private static final Logger log = LoggerFactory.getLogger(LoadBalanceServiceImpl.class);

    @Inject
    LbaasBackends backends;

    @Inject
    LbaasRepository repository;

    @Inject
    DnsService dns;

    @Override
    public void setMappings(String systemKey, Project project, List<LbaasMapping> mappings) throws CloudException {
        List<LbaasMappingData> records = LbaasMappingMapper.INSTANCE.toData(project, systemKey, mappings);

        setLbaasRecords(systemKey, project, records);
    }

    @Override
    public void setServers(String systemKey, Project project, List<LbaasServer> servers) throws CloudException {
        List<LbaasServerData> records = LbaasServerMapper.INSTANCE.toData(project, systemKey, servers);

        setLbaasServers(systemKey, project, records);
    }

    private void setLbaasRecords(String systemKey, Project project, List<LbaasMappingData> requests)
            throws CloudException {
        // This is where a database would be awesome!
        LbaasMappingMapper mapper = LbaasMappingMapper.INSTANCE;

        NumberedItemCollection<LbaasMappingData> collection = repository.getMappings(project);

        Map<LbaasMappingData, LbaasMappingData> rawToDbMap = Maps.newHashMap();
        Map<Long, LbaasMappingData> dbMap = Maps.newHashMap();
        for (LbaasMappingData mapping : collection.list()) {
            if (!mapping.hasSystemKey()) {
                continue;
            }

            if (!systemKey.equals(mapping.getSystemKey())) {
                continue;
            }

            rawToDbMap.put(mapper.toComparable(mapping), mapping);
            dbMap.put(mapping.getId(), mapping);
        }

        List<LbaasMappingData> add = Lists.newArrayList();
        Map<Long, LbaasMappingData> remove = Maps.newHashMap(dbMap);

        for (LbaasMappingData mapping : requests) {
            LbaasMappingData db = rawToDbMap.get(mapping);
            if (db == null) {
                add.add(mapping);
            } else {
                remove.remove(db.getId());
            }
        }

        Set<String> dirtyHosts = Sets.newHashSet();

        for (LbaasMappingData a : add) {
            LbaasMappingData.Builder b = LbaasMappingData.newBuilder(a);
            collection.create(b);

            dirtyHosts.add(b.getHost());
        }

        for (LbaasMappingData r : remove.values()) {
            collection.delete(r.getId());

            dirtyHosts.add(r.getHost());
        }

        for (String host : dirtyHosts) {
            updateHost(project, host);
        }
    }

    private void setLbaasServers(String systemKey, Project project, List<LbaasServerData> requests)
            throws CloudException {
        // This is where a database would be awesome!
        LbaasServerMapper mapper = LbaasServerMapper.INSTANCE;

        NumberedItemCollection<LbaasServerData> collection = repository.getServers(project);

        Map<LbaasServerData, LbaasServerData> rawToDbMap = Maps.newHashMap();
        Map<Long, LbaasServerData> dbMap = Maps.newHashMap();
        for (LbaasServerData mapping : collection.list()) {
            if (!mapping.hasSystemKey()) {
                continue;
            }

            if (!systemKey.equals(mapping.getSystemKey())) {
                continue;
            }

            rawToDbMap.put(mapper.toComparable(mapping), mapping);
            dbMap.put(mapping.getId(), mapping);
        }

        List<LbaasServerData> add = Lists.newArrayList();
        Map<Long, LbaasServerData> remove = Maps.newHashMap(dbMap);

        for (LbaasServerData mapping : requests) {
            LbaasServerData db = rawToDbMap.get(mapping);
            if (db == null) {
                add.add(mapping);
            } else {
                remove.remove(db.getId());
            }
        }

        Set<LbaasServerData> dirtyServers = Sets.newHashSet();

        for (LbaasServerData a : add) {
            LbaasServerData.Builder b = LbaasServerData.newBuilder(a);
            LbaasServerData created = collection.create(b);

            dirtyServers.add(created);
        }

        for (LbaasServerData r : remove.values()) {
            LbaasServerData deleted = collection.delete(r.getId());

            if (deleted == null) {
                throw new IllegalStateException("Did not find server during delete");
            }

            dirtyServers.add(deleted);
        }

        updateServers(project, dirtyServers);
    }

    private void updateServers(Project project, Set<LbaasServerData> dirtyServers) throws CloudException {
        // We need to update all the DNS records to point to the correct servers

        Set<String> hosts = Sets.newHashSet();
        for (LbaasMappingData mapping : repository.getMappings(project).list()) {
            // If we map hosts to specific servers, we'll do that here...
            hosts.add(mapping.getHost());
        }

        List<DnsRecordSpec> records = Lists.newArrayList();
        for (LbaasServerData server : repository.getServers(project).list()) {
            String ip = server.getIp();

            DnsRecordSpec record = new DnsRecordSpec();
            record.address = ip;

            records.add(record);
        }

        List<DnsRecordsetSpec> dnsRecordsets = Lists.newArrayList();
        for (String host : hosts) {
            DnsRecordsetSpec dnsRecord = new DnsRecordsetSpec();
            dnsRecord.fqdn = host;
            dnsRecord.type = "A";
            dnsRecord.records = Lists.newArrayList(records);

            dnsRecordsets.add(dnsRecord);
        }

        String systemKey = "__lb__/" + project.getId();
        dns.setDnsRecordsets(systemKey, project, dnsRecordsets);
    }

    private void updateHost(Project project, String host) throws CloudException {
        LbaasBackend backend = getBackend();
        backend.updateHost(project, host);
    }

    private LbaasBackend getBackend() throws CloudException {
        return backends.getBackend();
    }

    public List<LbaasMapping> listMappings(Project project, String host) throws CloudException {
        LbaasMappingMapper mapper = LbaasMappingMapper.INSTANCE;

        List<LbaasMapping> ret = Lists.newArrayList();

        for (LbaasMappingData mapping : repository.getMappings(project).list()) {
            if (!host.equals(mapping.getHost())) {
                continue;
            }
            ret.add(mapper.toModel(mapping));
        }

        return ret;
    }

}
