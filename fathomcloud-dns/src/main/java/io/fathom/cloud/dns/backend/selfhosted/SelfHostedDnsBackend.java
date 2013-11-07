package io.fathom.cloud.dns.backend.selfhosted;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.dns.backend.DnsBackendBase;
import io.fathom.cloud.dns.backend.selfhosted.model.ZoneData;
import io.fathom.cloud.dns.model.DnsZone;
import io.fathom.cloud.protobuf.DnsModel.BackendData;
import io.fathom.cloud.protobuf.DnsModel.DnsBackendProviderType;
import io.fathom.cloud.protobuf.DnsModel.DnsSuffixData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.storage.FileBlob;
import io.fathom.cloud.storage.StorageService;
import io.fathom.cloud.tasks.TaskScheduler;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import com.google.gson.Gson;

@Singleton
public class SelfHostedDnsBackend extends DnsBackendBase {
    @Inject
    TaskScheduler taskScheduler;

    @Inject
    StorageService storageService;

    @Inject
    Gson gson;

    @Override
    public void updateDomain(Project project, DnsZone domain) {
        UpdateSelfHosted job = new UpdateSelfHosted(project, domain);

        taskScheduler.execute(job);
    }

    @Override
    public String createZone(Project project, String zone, String topZone, DnsSuffixData suffixData) {
        // No-op?
        return null;
    }

    @Override
    public DnsBackendProviderType getType() {
        return DnsBackendProviderType.SELF_HOSTED;
    }

    public class UpdateSelfHosted extends UpdateDnsDomainBase {

        public UpdateSelfHosted(Project project, DnsZone domain) {
            super(project, domain);
        }

        @Override
        public Void call() throws CloudException, IOException {
            String zoneName = zone.getName();

            ZoneData zoneData = new ZoneData();
            zoneData.records = readFromDatabase(true);

            String path = "services/dns/__default/zones/" + zoneName;
            String bucket = "__services";

            String contents = gson.toJson(zoneData);

            ByteSource bytes = ByteSource.wrap(contents.getBytes(Charsets.UTF_8));

            String contentType = "application/json";
            Map<String, String> userAttributes = null;

            storageService.getFileService().ensureBucket(project, bucket);

            storageService.getFileService().putFile(project, bucket, path, FileBlob.build(bytes), contentType,
                    userAttributes);
            return null;
        }
    }

    public void init(BackendData backendData) {

    }

}
