package io.fathom.cloud.storage.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.CloudCommons.Attributes;
import io.fathom.cloud.protobuf.CloudCommons.KeyValueData;
import io.fathom.cloud.protobuf.FileModel.FileData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.server.model.User;
import io.fathom.cloud.storage.FileBlob;
import io.fathom.cloud.storage.FileServiceInternal;
import io.fathom.cloud.storage.FsBucket;
import io.fathom.cloud.storage.FsFile;
import io.fathom.cloud.storage.services.MimeTypes;

import java.io.File;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.utils.Hex;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

@Path("/openstack/storage/{project}/{bucket}/{name:.+}")
@Transactional
public class ObjectResource extends ObjectstoreResourceBase {
    private static final Logger log = LoggerFactory.getLogger(ObjectResource.class);

    private static final String OBJECT_META_PREFIX = "x-object-meta-";

    @PathParam("bucket")
    String bucketName;

    @PathParam("name")
    String name;

    @Inject
    FileServiceInternal fs;

    @PUT
    public Response createFile(File src) throws Exception {
        // TODO: Check bucket access before upload?
        Project project = getProject();

        // TODO: This doesn't work for even moderate-sized data

        String contentType = httpRequest.getHeader(HttpHeaders.CONTENT_TYPE);

        log.info("Create file {} with contentType {}", name, contentType);
        if (Strings.isNullOrEmpty(contentType)) {
            contentType = MimeTypes.INSTANCE.guessMimeType(name);
            log.info("Content type for file {} guessed as {}", name, contentType);
        }

        Map<String, String> userAttributes = Maps.newHashMap();

        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();

            // Header names are case-insensitive
            String normalized = headerName.toLowerCase();
            if (normalized.startsWith(OBJECT_META_PREFIX)) {
                String key = headerName.substring(OBJECT_META_PREFIX.length());
                String value = httpRequest.getHeader(headerName);

                userAttributes.put(key, value);
            }
        }

        FileBlob fileData = FileBlob.build(src);

        fs.putFile(project, bucketName, name, fileData, contentType, userAttributes);

        ResponseBuilder response = Response.status(Status.CREATED);
        return response.build();
    }

    @POST
    public Response appendToFile(File src) throws Exception {
        // TODO: Special handler for small data?

        // TODO: Check bucket access before upload?
        Project project = getProject();

        FileBlob fileData = FileBlob.build(src);

        // No position specified
        Long position = null;

        fs.append(project, bucketName, name, position, fileData);

        ResponseBuilder response = Response.status(Status.CREATED);
        return response.build();
    }

    @DELETE
    public Response deleteFile() throws Exception {
        fs.deleteFile(getProject(), bucketName, name);

        ResponseBuilder response = Response.status(Status.NO_CONTENT);
        return response.build();
    }

    public static Response buildReadResponse(HttpServletRequest httpRequest, FileServiceInternal fs, FsFile found) {
        ResponseBuilder response;

        if (found == null) {
            // TODO: Check for meta error page??
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        boolean head = httpRequest.getMethod().equalsIgnoreCase("head");

        boolean includeContentLength = true;

        if (head) {
            response = Response.ok();
        } else {
            String range = httpRequest.getHeader("Range");
            Long from = null;
            Long to = null;

            if (!Strings.isNullOrEmpty(range)) {
                if (range.startsWith("bytes=")) {
                    range = range.substring(6);
                    int dashIndex = range.indexOf('-');
                    if (dashIndex == -1) {
                        throw new IllegalArgumentException();
                    }
                    String fromString = range.substring(0, dashIndex);
                    String toString = range.substring(dashIndex + 1);

                    if (!Strings.isNullOrEmpty(fromString)) {
                        from = Long.valueOf(fromString);
                    }
                    if (!Strings.isNullOrEmpty(toString)) {
                        to = Long.valueOf(toString);
                    }
                    if (from == null && to != null) {
                        // This means the last N bytes
                        from = found.getLength() - to;
                        to = null;
                    }
                } else {
                    throw new UnsupportedOperationException();
                }
            }

            StreamingOutput stream = fs.open(found, from, to);

            if (from != null || to != null) {
                response = Response.status(206);

                long rangeStart = 0;

                if (from != null) {
                    rangeStart = Math.max(from, 0);
                }
                long rangeEnd = found.getLength() - 1;
                if (to != null) {
                    rangeEnd = Math.min(rangeEnd, to - 1);
                }
                String contentRange = "bytes " + rangeStart + "-" + rangeEnd + "/" + found.getLength();

                response.header(HttpHeaders.CONTENT_LENGTH, 1 + (rangeEnd - rangeStart));
                includeContentLength = false;

                response.header("Content-Range", contentRange);
            } else {
                response = Response.ok();
            }

            response.entity(stream);
        }

        setHeaders(found, response);

        // TODO: Support range with head??

        // We do always set the length, even on a HEAD
        // But on a range, it is specially handled!
        if (includeContentLength && found.getData().hasLength()) {
            response.header(HttpHeaders.CONTENT_LENGTH, found.getData().getLength());
        }

        response.header("Server", "JustInoughOpenStack");
        return response.build();
    }

    private FsFile findFile() throws CloudException {
        FsFile found;

        User user = null;

        FsBucket bucket;

        if (!isAuthenticated()) {
            // Check for a public file
            String projectName = getProjectName();
            Project project = new Project(Long.valueOf(projectName));
            bucket = fs.findBucket(user, project, bucketName);
        } else {
            user = getAuth().getUser();
            bucket = fs.findBucket(user, getProject(), bucketName);
        }

        if (bucket == null) {
            return null;
        }

        found = fs.findFileInfo(bucket, name);

        if (found == null && !isAuthenticated()) {
            // X-Container-Meta-Web-Index
            String indexPage = bucket.getMetaWebIndex();
            if (indexPage != null) {
                String index;
                if (!name.endsWith("/")) {
                    index = name + "/" + indexPage;
                } else {
                    index = name + indexPage;
                }

                log.debug("Page not found, so trying index page: {}", index);
                found = fs.findFileInfo(bucket, index);
            }
        }

        return found;
    }

    @HEAD
    public Response getFileHead() throws CloudException {
        FsFile found = findFile();
        return buildReadResponse(httpRequest, fs, found);
    }

    @GET
    public Response getFile() throws CloudException {
        FsFile found = findFile();
        return buildReadResponse(httpRequest, fs, found);
    }

    private static void setHeaders(FsFile found, ResponseBuilder response) {
        FileData data = found.getData();

        if (data.hasLastModified()) {
            response.lastModified(new Date(data.getLastModified()));
        }

        if (data.hasHash()) {
            response.header(HttpHeaders.ETAG, Hex.toHex(data.getHash().toByteArray()));
        }

        if (data.hasContentType()) {
            response.header(HttpHeaders.CONTENT_TYPE, data.getContentType());
        }

        if (data.hasAttributes()) {
            Attributes attributes = data.getAttributes();
            for (KeyValueData entry : attributes.getUserAttributesList()) {
                response.header(OBJECT_META_PREFIX + entry.getKey(), entry.getValue());
            }
        }
    }

}
