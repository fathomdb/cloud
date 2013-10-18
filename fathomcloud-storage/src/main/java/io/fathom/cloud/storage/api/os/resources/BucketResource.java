package io.fathom.cloud.storage.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.CloudCommons.Attributes;
import io.fathom.cloud.protobuf.CloudCommons.KeyValueData;
import io.fathom.cloud.protobuf.FileModel.BucketAttributes;
import io.fathom.cloud.protobuf.FileModel.BucketData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.server.model.User;
import io.fathom.cloud.storage.FileServiceInternal;
import io.fathom.cloud.storage.FsBucket;
import io.fathom.cloud.storage.FsFile;
import io.fathom.cloud.storage.api.os.models.StorageAcl;
import io.fathom.cloud.storage.api.os.models.StorageAcl.AclType;
import io.fathom.cloud.storage.state.FileStore;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

@Path("/openstack/storage/{project}/{bucket}")
@Transactional
public class BucketResource extends ObjectstoreResourceBase {
    private static final Logger log = LoggerFactory.getLogger(BucketResource.class);

    private static final String CONTAINER_META_PREFIX = "X-Container-Meta-";

    private static final String HEADER_ACL_READ = "X-Container-Read";

    @Inject
    FileStore fileStore;

    @PathParam("bucket")
    String bucketName;

    @QueryParam("marker")
    String marker;

    @QueryParam("prefix")
    String prefix;

    @QueryParam("delimiter")
    String delimiter;

    @Inject
    FileServiceInternal fs;

    @POST
    public Response createBucket() throws Exception {
        Project project = getProject();

        Map<String, String> userAttributes = Maps.newHashMap();

        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();

            // Header names are case-insensitive
            String normalized = headerName.toLowerCase();

            if (normalized.startsWith("x-container-meta-")) {
                String key = headerName.substring(17);
                String value = httpRequest.getHeader(headerName);

                userAttributes.put(key, value);
            }
        }

        BucketAttributes.Builder bucketAttributes = BucketAttributes.newBuilder();
        {
            String header = httpRequest.getHeader(HEADER_ACL_READ);
            if (header != null) {
                StorageAcl acl = StorageAcl.parse(AclType.Read, header);
                bucketAttributes.setAclRead(acl.toString());
            }
        }

        Status status = fs.putBucket(project, bucketName, bucketAttributes.build(), userAttributes);

        ResponseBuilder response = Response.status(status);
        return response.build();
    }

    @PUT
    public Response createBucketPut() throws Exception {
        return createBucket();
    }

    @DELETE
    public Response deleteBucket() throws Exception {
        Project project = getProject();

        fs.deleteBucket(project, bucketName);

        ResponseBuilder response = Response.status(Status.NO_CONTENT);
        return response.build();
    }

    // TODO: Support HEAD directly (efficiently)
    @GET
    public Response listObjects(@QueryParam("format") String formatParam) throws CloudException {
        if (!isAuthenticated()) {
            return doAnonymousRead();
        }

        Project project = getProject();

        User user = getAuth().getUser();
        FsBucket bucket = fs.findBucket(user, project, bucketName);
        if (bucket == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        List<DirectoryListEntry> entries = fs.listFiles(project, bucket, prefix, delimiter, marker);

        ResponseBuilder response = Response.ok();

        // X-Container-Object-Count: 7
        // X-Container-Bytes-Used: 413

        setHeaders(bucket.getData(), response);

        if (formatParam == null) {
            response.entity(new ObjectListTextWriter(entries));
        } else if ("json".equals(formatParam)) {
            response.entity(new ObjectListJsonWriter(entries));
        } else {
            throw new UnsupportedOperationException();
        }

        return response.build();
    }

    private Response doAnonymousRead() throws CloudException {
        FsFile found = null;

        FsBucket bucket;
        User user = null;

        // Check for a public file
        String projectName = getProjectName();
        Project project = new Project(Long.valueOf(projectName));
        bucket = fs.findBucket(user, project, bucketName);

        if (bucket != null) {
            // X-Container-Meta-Web-Index
            String indexPage = bucket.getMetaWebIndex();
            if (indexPage != null) {
                found = fs.findFileInfo(bucket, indexPage);
            }
        }

        return ObjectResource.buildReadResponse(httpRequest, fs, found);
    }

    private void setHeaders(BucketData found, ResponseBuilder response) {
        // if (found.hasLastModified()) {
        // response.lastModified(new Date(found.getLastModified()));
        // }
        //
        // if (found.hasHash()) {
        // response.header(HttpHeaders.ETAG,
        // Hex.toHex(found.getHash().toByteArray()));
        // }

        // if (found.hasContentType()) {
        // response.header(HttpHeaders.CONTENT_TYPE, found.getContentType());
        // }

        if (found.hasBucketAttributes()) {
            BucketAttributes bucketAttributes = found.getBucketAttributes();
            if (bucketAttributes.hasAclRead()) {
                response.header(HEADER_ACL_READ, bucketAttributes.getAclRead());
            }
        }

        // X-Container-Meta-Web-Error
        // X-Container-Meta-Web-Index
        // X-Container-Meta-Web-Listings
        // X-Container-Meta-Web-Listings-CSS
        // X-Container-Meta-InspectedBy: JackWolf
        if (found.hasAttributes()) {
            Attributes attributes = found.getAttributes();
            for (KeyValueData entry : attributes.getUserAttributesList()) {
                response.header(CONTAINER_META_PREFIX + entry.getKey(), entry.getValue());
            }
        }
        // TODO: How should we set the length... Maybe only on a head?
        // if (found.hasLength()) {
        // response.header(HttpHeaders.CONTENT_LENGTH, found.getLength());
        // }
    }
}
