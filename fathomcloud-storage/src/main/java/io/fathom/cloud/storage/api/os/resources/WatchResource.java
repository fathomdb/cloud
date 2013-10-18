package io.fathom.cloud.storage.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.server.model.User;
import io.fathom.cloud.storage.FileServiceInternal;
import io.fathom.cloud.storage.FsBucket;
import io.fathom.cloud.tasks.TaskScheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

@Path("/openstack/storage/watch/{project}")
public class WatchResource extends ObjectstoreResourceBase {
    private static final Logger log = LoggerFactory.getLogger(WatchResource.class);

    @Inject
    FileServiceInternal fs;

    @PathParam("project")
    String projectName;

    @PathParam("bucket")
    String bucketName;

    @Inject
    TaskScheduler schedulerService;

    @GET
    @Path("{bucket}")
    public void watchBucket(@Suspended final AsyncResponse response, @QueryParam("since") String since)
            throws CloudException {
        User user = getAuth().getUser();

        Project project = findProject(projectName);
        if (project == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        FsBucket bucket = fs.findBucket(user, project, bucketName);
        if (bucket == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        ListenableFuture<?> future = fs.watchBucket(bucket, since);
        future.addListener(new Runnable() {
            @Override
            public void run() {
                response.resume(Response.ok().build());
            }
        }, schedulerService.getExecutor());

        response.setTimeoutHandler(new TimeoutHandler() {
            @Override
            public void handleTimeout(AsyncResponse ar) {
                response.resume(Response.status(Status.SERVICE_UNAVAILABLE)
                        .entity("Operation timed out -- please try again").build());
                response.resume(new WebApplicationException(503));
            }
        });
        response.setTimeout(5, TimeUnit.SECONDS);
    }

    // @GET
    // @Path("hello")
    // public void test(@Suspended final AsyncResponse response) throws
    // CloudException {
    // schedulerService.schedule(new Runnable() {
    //
    // @Override
    // public void run() {
    // response.resume(Response.ok().entity("Hello world @" +
    // System.currentTimeMillis()).build());
    // }
    //
    // }, TimeSpan.TEN_SECONDS);
    //
    // log.info("Suspending");
    // }
}
