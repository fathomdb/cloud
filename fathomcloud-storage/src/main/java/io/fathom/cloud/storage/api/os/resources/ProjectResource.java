package io.fathom.cloud.storage.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.FileModel.BucketData;
import io.fathom.cloud.storage.state.FileStore;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.inject.persist.Transactional;

@Path("/openstack/storage/{project}")
@Transactional
public class ProjectResource extends ObjectstoreResourceBase {
    private static final Logger log = LoggerFactory.getLogger(ProjectResource.class);

    @Inject
    FileStore fileStore;

    @QueryParam("marker")
    String marker;

    @HEAD
    public Response getStats() throws CloudException {
        // String projectId = getProjectId();

        long containerCount = 0;
        long bytesUsed = 0;

        log.warn("Container stats not implemented");

        ResponseBuilder response = Response.noContent();
        response.header("X-Account-Container-Count", Long.toString(containerCount));
        response.header("X-Account-Bytes-Used", Long.toString(bytesUsed));
        response.header("Date", new Date());
        return response.build();

    }

    @GET
    public StreamingOutput listContainers(@QueryParam("format") String formatParam) throws CloudException {
        if ("json".equals(formatParam)) {

        } else {
            throw new UnsupportedOperationException();
        }

        long projectId = getProject().getId();

        List<BucketData> buckets = fileStore.getBuckets(projectId).list();

        Collections.sort(buckets, Ordering.natural().onResultOf(new Function<BucketData, String>() {
            @Override
            public String apply(BucketData input) {
                return input.getKey();
            }
        }));

        List<BucketData> filtered = buckets;

        if (marker != null) {
            log.warn("Marker query is inefficient");
            List<BucketData> afterLimit = Lists.newArrayList();

            for (BucketData bucket : buckets) {
                String name = bucket.getKey();
                if (name.compareTo(marker) <= 0) {
                    continue;
                }

                afterLimit.add(bucket);
            }
            filtered = afterLimit;
        }

        return new JsonBucketListWriter(filtered);

    }

}
