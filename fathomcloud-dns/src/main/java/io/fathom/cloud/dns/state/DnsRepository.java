package io.fathom.cloud.dns.state;

import io.fathom.cloud.protobuf.DnsModel.BackendData;
import io.fathom.cloud.protobuf.DnsModel.DnsRecordsetData;
import io.fathom.cloud.protobuf.DnsModel.DnsSuffixData;
import io.fathom.cloud.protobuf.DnsModel.DnsZoneData;
import io.fathom.cloud.state.NamedItemCollection;
import io.fathom.cloud.state.NumberedItemCollection;
import io.fathom.cloud.state.RepositoryBase;
import io.fathom.cloud.state.StateStore.StateNode;

import javax.inject.Singleton;

@Singleton
public class DnsRepository extends RepositoryBase {
    public NamedItemCollection<DnsSuffixData> getDnsSuffixes() {
        StateNode node = stateStore.getRoot("dnssuffix");
        return NamedItemCollection.builder(node, DnsSuffixData.class).idField(DnsSuffixData.KEY_FIELD_NUMBER).create();
    }

    public NamedItemCollection<DnsSuffixData> getSharedSubdomains(String key) {
        StateNode subdomains = stateStore.getRoot("dnssubdomains");
        StateNode node = subdomains.child(key);

        return NamedItemCollection.builder(node, DnsSuffixData.class).idField(DnsSuffixData.KEY_FIELD_NUMBER).create();
    }

    public NumberedItemCollection<DnsZoneData> getDnsZones(long projectId) {
        StateNode dnsNode = stateStore.getRoot("dnsdomain");
        StateNode projectNode = dnsNode.child(Long.toHexString(projectId));
        return NumberedItemCollection.builder(projectNode, DnsZoneData.class).idField(DnsZoneData.ID_FIELD_NUMBER)
                .keyField(DnsZoneData.NAME_FIELD_NUMBER).create();
    }

    public NumberedItemCollection<DnsRecordsetData> getDnsRecordsets(long projectId, long zoneId) {
        StateNode dnsNode = stateStore.getRoot("dnsrecord");
        StateNode projectNode = dnsNode.child(Long.toHexString(projectId));
        StateNode domainNode = projectNode.child(Long.toHexString(zoneId));

        return NumberedItemCollection.builder(domainNode, DnsRecordsetData.class)
                .idField(DnsRecordsetData.ID_FIELD_NUMBER).create();
    }

    public NamedItemCollection<BackendData> getBackends() {
        StateNode node = stateStore.getRoot("dnsbackends");

        return NamedItemCollection.builder(node, BackendData.class).idField(BackendData.KEY_FIELD_NUMBER).create();
    }

}
