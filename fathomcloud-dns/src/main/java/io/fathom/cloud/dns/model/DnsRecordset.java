package io.fathom.cloud.dns.model;

import io.fathom.cloud.dns.DnsService;
import io.fathom.cloud.protobuf.DnsModel.DnsRecordData;
import io.fathom.cloud.protobuf.DnsModel.DnsRecordsetData;

import java.util.List;

import com.google.common.collect.Lists;

public class DnsRecordset implements DnsService.Recordset {
    final DnsZone domain;
    final DnsRecordsetData data;

    public DnsRecordset(DnsZone domain, DnsRecordsetData data) {
        this.domain = domain;
        this.data = data;
    }

    @Override
    public DnsZone getZone() {
        return domain;
    }

    public DnsRecordsetData getData() {
        return data;
    }

    @Override
    public List<? extends DnsRecord> getRecords() {
        List<DnsRecord> records = Lists.newArrayList();

        for (DnsRecordData record : data.getRecordList()) {
            records.add(new DnsRecord(this, record));
        }
        return records;
    }

    @Override
    public String getFqdn() {
        return data.getFqdn();
    }

    @Override
    public String getType() {
        return data.getType();
    }

    // public String getFqdn() {
    // String domainName = domain.getName();
    //
    // String fqdn;
    // if (!Strings.isNullOrEmpty(data.getName())) {
    // fqdn = data.getName() + "." + domainName;
    // } else {
    // fqdn = domainName;
    // }
    //
    // return fqdn;
    // }

    public static List<DnsRecordsetData> toData(List<DnsRecordset> items) {
        if (items == null) {
            return null;
        }

        List<DnsRecordsetData> ret = Lists.newArrayList();
        if (items != null) {
            for (DnsRecordset item : items) {
                ret.add(item.getData());
            }
        }

        return ret;
    }

}
