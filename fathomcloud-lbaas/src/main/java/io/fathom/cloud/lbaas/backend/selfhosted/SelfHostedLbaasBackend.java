package io.fathom.cloud.lbaas.backend.selfhosted;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.lbaas.backend.LbaasBackendBase;
import io.fathom.cloud.lbaas.services.Escaping;
import io.fathom.cloud.openstack.client.loadbalance.model.LbaasMapping;
import io.fathom.cloud.openstack.client.loadbalance.model.LoadBalanceMappingList;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.storage.FileBlob;
import io.fathom.cloud.storage.StorageService;
import io.fathom.cloud.tasks.TaskScheduler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import com.google.gson.Gson;

@Singleton
public class SelfHostedLbaasBackend extends LbaasBackendBase {
    @Inject
    TaskScheduler taskScheduler;

    @Inject
    StorageService storageService;

    @Inject
    Gson gson;

    @Override
    public void updateHost(Project project, String host) {
        UpdateSelfHosted task = new UpdateSelfHosted(project, host);
        taskScheduler.execute(task);
    }

    public class UpdateSelfHosted implements Callable<Void> {

        private final Project project;
        private final String host;

        public UpdateSelfHosted(Project project, String host) {
            this.project = project;
            this.host = host;
        }

        @Override
        public Void call() throws CloudException, IOException {
            LoadBalanceMappingList data = new LoadBalanceMappingList();

            List<LbaasMapping> mappings = lbaas.listMappings(project, host);
            data.mappings = mappings;

            String path = "__default/lb/data/" + Escaping.escape(host);
            String bucket = "__services";

            String contents = gson.toJson(data);

            ByteSource bytes = ByteSource.wrap(contents.getBytes(Charsets.UTF_8));

            String contentType = "application/json";
            Map<String, String> userAttributes = null;

            storageService.getFileService().putFile(project, bucket, path, FileBlob.build(bytes), contentType,
                    userAttributes);
            return null;

        }
    }

}
