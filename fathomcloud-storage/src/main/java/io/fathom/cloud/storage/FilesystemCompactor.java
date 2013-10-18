package io.fathom.cloud.storage;

import io.fathom.cloud.server.model.Project;

import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.TimeSpan;

@Singleton
public class FilesystemCompactor {

    private static final Logger log = LoggerFactory.getLogger(FilesystemCompactor.class);

    @Inject
    FileServiceInternal fileService;

    final LinkedBlockingQueue<CompactOperation> queue = new LinkedBlockingQueue<>();

    public void enqueue(Project project, String bucketName, String name) {
        // TODO: Don't enqueue if already in queue?
        CompactOperation key = new CompactOperation(project, bucketName, name);
        try {
            queue.put(key);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Error while adding to change queue", e);
        }
    }

    public void start() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        poll();
                    } catch (Exception e) {
                        log.error("Error during polling", e);
                    }
                    TimeSpan.FIVE_SECONDS.doSafeSleep();
                }
            }
        });
        thread.start();
    }

    protected void poll() throws InterruptedException {
        while (true) {
            // TODO: Figure out how this should work when distributed to avoid
            // repeating work
            CompactOperation compaction = queue.take();

            try {
                if (!fileService.compact(compaction)) {
                    // TODO: Don't enqueue if already in queue?
                    log.debug("Compaction was not needed: {}", compaction);
                } else {
                    log.debug("Compaction successful: {}", compaction);
                }
            } catch (Exception e) {
                // TODO: Auto-retry on concurrent modification?
                // TODO: So we can re-use the compacted data
                // TODO: We probably want a worker pool
                log.warn("Ignoring error while compacting: " + compaction, e);
            }
        }
    }

}
