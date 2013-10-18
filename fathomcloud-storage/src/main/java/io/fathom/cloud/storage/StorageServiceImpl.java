package io.fathom.cloud.storage;

import io.fathom.cloud.blobs.replicated.UpdateClusterTask;
import io.fathom.cloud.lifecycle.LifecycleListener;
import io.fathom.cloud.storage.FileService;
import io.fathom.cloud.storage.StorageService;
import io.fathom.cloud.tasks.TaskScheduler;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StorageServiceImpl implements StorageService, LifecycleListener {

    @Inject
    TaskScheduler scheduler;

    @Inject
    FilesystemCompactor compactor;

    @Inject
    FileService fileService;

    @Override
    public void start() throws Exception {
        scheduler.schedule(UpdateClusterTask.class);

        compactor.start();
    }

    @Override
    public FileService getFileService() {
        return fileService;
    }

}
