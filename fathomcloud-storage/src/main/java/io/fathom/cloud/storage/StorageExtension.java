package io.fathom.cloud.storage;

import io.fathom.cloud.storage.FileService;
import io.fathom.cloud.storage.StorageService;
import io.fathom.cloud.storage.api.os.resources.BucketResource;
import io.fathom.cloud.storage.api.os.resources.DevResource;
import io.fathom.cloud.storage.api.os.resources.ObjectResource;
import io.fathom.cloud.storage.api.os.resources.ProjectResource;
import io.fathom.cloud.storage.api.os.resources.WatchResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.extensions.ExtensionModuleBase;
import com.fathomdb.extensions.HttpConfiguration;

public class StorageExtension extends ExtensionModuleBase {

    private static final Logger log = LoggerFactory.getLogger(StorageExtension.class);

    @Override
    public void addHttpExtensions(HttpConfiguration http) {
        // boolean ENABLE_S3 = false;
        // if (ENABLE_S3) {
        // http.bind(S3Resource.class);
        // }
        //
        // if (ENABLE_S3) {
        // http.filter("/*").through(AwsFilter.class);
        // }

        http.bind(ProjectResource.class);
        http.bind(BucketResource.class);
        http.bind(ObjectResource.class);
        http.bind(WatchResource.class);

        log.warn("Binding development mode resources");
        http.bind(DevResource.class);
    }

    @Override
    protected void configure() {
        bind(StorageService.class).to(StorageServiceImpl.class);
        bind(FileService.class).to(FileServiceImpl.class);
        bind(FileServiceInternal.class).to(FileServiceImpl.class);
        bind(StorageDerivedMetadata.class).to(StorageDerivedMetadataImpl.class);
    }

}
