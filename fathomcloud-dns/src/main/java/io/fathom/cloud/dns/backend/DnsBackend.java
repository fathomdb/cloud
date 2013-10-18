package io.fathom.cloud.dns.backend;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.dns.model.DnsZone;
import io.fathom.cloud.protobuf.DnsModel.DnsBackendProviderType;
import io.fathom.cloud.protobuf.DnsModel.DnsSuffixData;
import io.fathom.cloud.server.model.Project;

public interface DnsBackend {

    void updateDomain(Project project, DnsZone domain);

    String createZone(Project project, String zone, String topZone, DnsSuffixData suffixData) throws CloudException;

    DnsBackendProviderType getType();
}
