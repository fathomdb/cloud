package io.fathom.cloud.dns.backend.aws;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.dns.backend.DnsBackendBase;
import io.fathom.cloud.dns.model.DnsZone;
import io.fathom.cloud.dns.services.DnsSecrets;
import io.fathom.cloud.openstack.client.dns.model.Record;
import io.fathom.cloud.openstack.client.dns.model.Recordset;
import io.fathom.cloud.protobuf.DnsModel.BackendData;
import io.fathom.cloud.protobuf.DnsModel.BackendSecretData;
import io.fathom.cloud.protobuf.DnsModel.DnsBackendProviderType;
import io.fathom.cloud.protobuf.DnsModel.DnsSuffixData;
import io.fathom.cloud.server.model.Project;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Singleton
public class AwsDnsBackend extends DnsBackendBase {

    private static final Logger log = LoggerFactory.getLogger(AwsDnsBackend.class);

    @Inject
    DnsSecrets dnsSecrets;

    @Inject
    AwsExecutor awsExecutor;

    AwsRoute53Client client;

    public void init(BackendData backendData) {
        BackendSecretData secretData = dnsSecrets.getSecretData(backendData);

        String accessKey = secretData.getUsername();
        String secretKey = secretData.getPassword();

        client = new AwsRoute53Client(accessKey, secretKey);
    }

    @Override
    public void updateDomain(Project project, DnsZone zone) {
        UpdateRoute53 job = new UpdateRoute53(project, zone, client);

        awsExecutor.execute(job);
    }

    @Override
    public String createZone(Project project, String zone, String topZone, DnsSuffixData suffixData) {
        if (suffixData.getSharedDomain()) {
            log.info("Request to create {} under shared AWS Route 53 domain {}; will reuse", zone, suffixData.getKey());
            return null;
        } else {
            HostedZone hostedZone = client.createHostedZone(topZone);
            return hostedZone.getId();
        }
    }

    @Override
    public DnsBackendProviderType getType() {
        return DnsBackendProviderType.AWS_ROUTE53;
    }

    public class UpdateRoute53 extends UpdateDnsDomainBase {
        protected final AwsRoute53Client client;

        public UpdateRoute53(Project project, DnsZone zone, AwsRoute53Client client) {
            super(project, zone);
            this.client = client;
        }

        @Override
        public Void call() throws CloudException, IOException {
            log.debug("Updating domain: {}", zone.getName());

            List<Recordset> requested = readFromDatabase(false);

            HostedZone hostedZone = getHostedZone();

            List<Recordset> aws = readFromAws(hostedZone);

            Changes changes = computeChanges(aws, requested);

            List<ResourceRecordSet> create = mapToAws(hostedZone.getName(), changes.create);
            List<ResourceRecordSet> remove = mapToAws(hostedZone.getName(), changes.remove);
            client.changeRecords(hostedZone.getId(), create, remove);

            return null;
        }

        private List<ResourceRecordSet> mapToAws(String hostedZoneName, List<Recordset> list) {
            List<ResourceRecordSet> ret = Lists.newArrayList();
            for (Recordset r : list) {
                String name = r.name;
                if (!name.endsWith(".")) {
                    name += ".";
                }

                if ("SOA".equals(r.type)) {
                    // ignore if not at the root
                    if (!name.equals(hostedZoneName)) {
                        log.info("Ignoring SOA record not at root of AWS hosted zone: {}", name);
                        continue;
                    }
                }

                ResourceRecordSet rrs = new ResourceRecordSet();
                rrs.setName(name);
                rrs.setWeight(r.weight);

                Long ttl = r.ttl;
                if (ttl == null) {
                    ttl = 600L;
                }
                rrs.setTTL(ttl);

                rrs.setType(r.type);

                // rrs.setSetIdentifier("openstack:" + r.zone_id + ":" + r.id);

                List<ResourceRecord> resourceRecords = Lists.newArrayList();
                Set<String> values = Sets.newHashSet();

                for (Record record : r.records) {
                    String value = record.value;
                    // These get encoded into a string for route 53
                    if (record.port != null) {
                        throw new IllegalStateException();
                    }
                    if (record.priority != null) {
                        throw new IllegalStateException();
                    }
                    if (record.weight != null) {
                        throw new IllegalStateException();
                    }

                    if (values.contains(value)) {
                        log.debug("Skipping duplicate value: {}", value);
                        continue;
                    }
                    values.add(value);

                    ResourceRecord rr = new ResourceRecord();
                    rr.setValue(record.value);
                    resourceRecords.add(rr);
                }
                rrs.setResourceRecords(resourceRecords);

                ret.add(rrs);
            }
            return ret;
        }

        HostedZone getHostedZone() {
            Map<String, HostedZone> hostedZones = client.getHostedZones();

            String zoneName = zone.getName();
            if (!zoneName.endsWith(".")) {
                zoneName += ".";
            }

            String awsZoneName = client.getAwsZoneName(zoneName);
            HostedZone hostedZone = hostedZones.get(awsZoneName);
            if (hostedZone == null) {
                hostedZone = client.createHostedZone(awsZoneName);
            }
            return hostedZone;
        }

        private List<Recordset> readFromAws(HostedZone hostedZone) {
            log.debug("Reading AWS zone: {}", hostedZone.getName());

            List<Recordset> recordsets = Lists.newArrayList();

            String domainName = CharMatcher.is('.').trimTrailingFrom(hostedZone.getName());
            String awsZoneId = hostedZone.getId();

            // String tagPrefix = "openstack:" + domain.getId() + ":";

            List<ResourceRecordSet> awsRecordsets = client.getResourceRecords(awsZoneId);
            for (ResourceRecordSet awsRecordset : awsRecordsets) {
                boolean sameDomain = false;
                String awsRecordsetName = CharMatcher.is('.').trimTrailingFrom(awsRecordset.getName());
                if (awsRecordsetName.equals(domainName)) {
                    sameDomain = true;
                } else if (awsRecordsetName.endsWith("." + domainName)) {
                    sameDomain = true;
                    // // Don't match www.sub.domain.com against domain.com
                    // String prefix = rrsDomainName.substring(0,
                    // rrsDomainName.length() - (domainName.length() + 1));
                    // int dotIndex = prefix.indexOf('.');
                    // if (dotIndex == -1) {
                    // sameDomain = true;
                    // }
                }
                // String tag = awsRecordset.getSetIdentifier();
                // if (Strings.isNullOrEmpty(tag)) {
                // log.warn("Record did not have tag: {}",
                // awsRecordset.getName());
                // } else {
                // if (tag.startsWith(tagPrefix)) {
                // sameDomain = true;
                // }
                // }

                if (!sameDomain) {
                    log.debug("Domain name not part of domain: {}", awsRecordsetName);
                    continue;
                }

                Recordset recordset = new Recordset();

                recordset.type = awsRecordset.getType();
                recordset.name = awsRecordsetName;

                recordset.ttl = awsRecordset.getTTL();
                recordset.weight = awsRecordset.getWeight();
                recordset.records = Lists.newArrayList();
                for (ResourceRecord awsRR : awsRecordset.getResourceRecords()) {
                    Record rr = new Record();
                    rr.value = awsRR.getValue();
                    if (rr.value.contains(" ")) {
                        List<String> tokens = Splitter.on(' ').splitToList(rr.value);

                        if (recordset.type.equals("SOA") && tokens.size() == 7) {
                            // Ignore for now
                            // String ns
                            // ns-310.awsdns-38.com.
                            // awsdns-hostmaster.amazon.com. 1
                            // 7200 900 1209600 86400
                        } else {
                            throw new IllegalStateException("Cannot decode route 53 value: " + awsRR);
                        }
                    }
                    recordset.records.add(rr);
                }

                recordsets.add(recordset);
            }

            return recordsets;
        }

        @Override
        protected List<Recordset> readFromDatabase(boolean createSoa) throws CloudException {
            List<Recordset> recordsets = super.readFromDatabase(createSoa);

            Map<String, Recordset> unique = Maps.newHashMap();

            for (Recordset recordset : recordsets) {
                String key = recordset.type + ":" + recordset.name;
                if (recordset.weight != null) {
                    key += ":" + recordset.id;
                }
                Recordset existing = unique.get(key);
                if (existing == null) {
                    unique.put(key, recordset);
                } else {
                    Recordset merged = merge(existing, recordset);
                    unique.put(key, merged);
                }
            }
            return Lists.newArrayList(unique.values());
        }

        Recordset merge(Recordset a, Recordset b) {
            if (a.isDeleted()) {
                return b;
            }
            if (b.isDeleted()) {
                return a;
            }
            Recordset merged = new Recordset();
            merged.name = a.name;
            merged.type = a.type;
            merged.records = Lists.newArrayList();
            merged.records.addAll(a.records);
            merged.records.addAll(b.records);
            merged.ttl = a.ttl;
            merged.weight = a.weight;
            merged.zone_id = a.zone_id;
            merged.version = a.version;
            return merged;
        }
    }
}
