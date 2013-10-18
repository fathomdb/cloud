package io.fathom.cloud.dns.model;

import io.fathom.cloud.dns.DnsService;
import io.fathom.cloud.protobuf.DnsModel.DnsZoneData;

import java.util.List;

import com.google.common.collect.Lists;

public class DnsZone implements DnsService.Zone {
    final DnsZoneData data;

    public DnsZone(DnsZoneData data) {
        this.data = data;
    }

    @Override
    public long getProjectId() {
        return data.getProjectId();
    }

    @Override
    public String getName() {
        return data.getName();
    }

    public DnsZoneData getData() {
        return data;
    }

    @Override
    public long getId() {
        return data.getId();
    }

    public static List<DnsZoneData> toData(List<DnsZone> domains) {
        if (domains == null) {
            return null;
        }

        List<DnsZoneData> ret = Lists.newArrayList();
        if (domains != null) {
            for (DnsZone domain : domains) {
                ret.add(domain.getData());
            }
        }

        return ret;
    }

}
