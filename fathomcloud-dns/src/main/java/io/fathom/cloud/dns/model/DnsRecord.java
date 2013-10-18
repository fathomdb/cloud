package io.fathom.cloud.dns.model;

import io.fathom.cloud.dns.DnsService;
import io.fathom.cloud.protobuf.DnsModel.DnsRecordData;

public class DnsRecord implements DnsService.Record {
    final DnsRecordset recordset;
    final DnsRecordData data;

    public DnsRecord(DnsRecordset recordset, DnsRecordData data) {
        this.recordset = recordset;
        this.data = data;
    }

    public DnsRecordset getRecordset() {
        return recordset;
    }

    public DnsRecordData getData() {
        return data;
    }

    @Override
    public String getTarget() {
        return data.getTarget();
    }

}
