package io.fathom.cloud.image;

import io.fathom.cloud.Clock;
import io.fathom.cloud.CloudException;
import io.fathom.cloud.blobs.BlobData;
import io.fathom.cloud.image.api.os.resources.ImageFilter;
import io.fathom.cloud.protobuf.CloudCommons.Attributes;
import io.fathom.cloud.protobuf.CloudCommons.KeyValueData;
import io.fathom.cloud.protobuf.ImageModel.ImageData;
import io.fathom.cloud.protobuf.ImageModel.ImageLocation;
import io.fathom.cloud.protobuf.ImageModel.ImageState;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.server.resources.Urls;
import io.fathom.cloud.services.ImageKey;
import io.fathom.cloud.services.ImageService;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import com.google.inject.persist.Transactional;
import com.google.protobuf.Descriptors.FieldDescriptor;

@Singleton
@Transactional
public class ImageServiceImpl implements ImageService {
    private static final Logger log = LoggerFactory.getLogger(ImageServiceImpl.class);

    private static final Comparator<Image> ORDER_BY_NAME = new Comparator<Image>() {
        @Override
        public int compare(Image o1, Image o2) {
            String s1 = o1.getName();
            String s2 = o2.getName();

            if (s1 == null) {
                return s2 == null ? 0 : -1;
            }

            if (s2 == null) {
                return 1;
            }

            return s1.compareTo(s2);
            //
            //
            // long v1 = o1.getId();
            // long v2 = o2.getId();
            // return Long.compare(v1, v2);
        }
    };

    @Inject
    ImageRepository imageRepository;

    @Inject
    ImageDataService imageDataService;

    public List<Image> listImages(Project project, ImageFilter filter) throws CloudException {
        List<Image> matching = Lists.newArrayList();
        for (ImageData image : imageRepository.getImages().list()) {
            if (!isVisible(project, image)) {
                continue;
            }

            if (!filter.matches(image)) {
                continue;
            }

            matching.add(new ImageImpl(image));
        }

        Collections.sort(matching, ORDER_BY_NAME);
        return matching;
    }

    @Override
    public List<Image> listImages(Project project) throws CloudException {
        ImageFilter filter = new ImageFilter();
        return listImages(project, filter);
    }

    @Override
    public ImageImpl findImage(Project project, long imageId) throws CloudException {
        ImageData image = imageRepository.getImages().find(imageId);
        if (image != null) {
            if (!isVisible(project, image)) {
                image = null;
            }
        }
        if (image == null) {
            return null;
        }
        return new ImageImpl(image);
    }

    private boolean isVisible(Project project, ImageData image) {
        if (image.hasImageState()) {
            switch (image.getImageState()) {
            case DELETED:
                return false;

            default:
                // Not blocked
                break;
            }
        }

        if (image.getIsPublic()) {
            return true;
        }
        if (image.getOwnerProject() == project.getId()) {
            return true;
        }
        return false;
    }

    public ImageData deleteImage(Project project, long id) throws CloudException {
        ImageImpl i = findImage(project, id);
        if (i == null) {
            // We check because it might not be visible!
            return null;
        }

        ImageData.Builder b = ImageData.newBuilder(i.getData());
        b.setImageState(ImageState.DELETED);
        b.setDeletedAt(Clock.getTimestamp());

        // TODO: Clean up old deleted images?
        // TODO: Remove from storage?

        return imageRepository.getImages().update(b);
    }

    public ImageData changeTags(Project project, int id, List<String> addTags, List<String> removeTags)
            throws CloudException {
        ImageImpl i = findImage(project, id);
        if (i == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        ImageData.Builder b = ImageData.newBuilder(i.getData());

        List<String> tags = Lists.newArrayList(b.getTagList());

        for (String addTag : addTags) {
            if (tags.contains(addTag)) {
                continue;
            }
            tags.add(addTag);
        }

        tags.removeAll(removeTags);

        b.clearTag();
        b.addAllTag(tags);

        return imageRepository.getImages().update(b);
    }

    public ImageData updateImage(Project project, long id, Map<String, String> metadata) throws CloudException {
        ImageImpl i = findImage(project, id);
        if (i == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        ImageData changes;

        {
            ImageData.Builder b = ImageData.newBuilder();
            mapHeadersToBuilder(metadata, b);
            changes = b.buildPartial();
        }

        Set<String> allowed = Sets.newHashSet("is_public", "name", "attributes", "is_protected");

        for (Entry<FieldDescriptor, Object> entry : changes.getAllFields().entrySet()) {
            FieldDescriptor field = entry.getKey();
            String key = field.getName();

            if (!allowed.contains(key)) {
                Object existing = i.getData().getField(field);
                if (!Objects.equal(existing, entry.getValue())) {
                    log.warn("Attempt to update blocked field: " + key);
                    throw new IllegalArgumentException();
                }
            }
        }

        ImageData.Builder b = ImageData.newBuilder(i.getData());

        b.mergeFrom(changes);

        removeRepeatedAttributes(b.getAttributesBuilder());

        return imageRepository.getImages().update(b);
    }

    private void removeRepeatedAttributes(Attributes.Builder attributes) {
        Map<String, KeyValueData> map = Maps.newLinkedHashMap();

        for (KeyValueData kv : attributes.getUserAttributesList()) {
            if (!kv.hasKey()) {
                continue;
            }

            map.put(kv.getKey(), kv);
        }

        if (map.size() != attributes.getUserAttributesCount()) {
            attributes.clearUserAttributes();
            for (KeyValueData kv : map.values()) {
                attributes.addUserAttributes(kv);
            }
        }
    }

    public BlobData findImageData(Project project, ImageData image) throws IOException {
        ImageLocation imageLocation = image.getLocation();
        if (imageLocation.hasStored()) {
            String cookie = imageLocation.getStored();

            BlobData blob = imageDataService.getImageFile(cookie);

            return blob;
        }
        return null;
    }

    // public void changeMembers(long id, List<String> addMembers,
    // List<String> removeMembers) {
    // ImageData i = findImage(id);
    // if (i == null) {
    // throw new WebApplicationException(Status.NOT_FOUND);
    // }
    //
    // ImageData.Builder b = ImageData.newBuilder(i);
    //
    // List<String> members = Lists.newArrayList(b.getMembers());
    //
    // for (String removeMember : removeMembers) {
    // if (members.contains(removeMember)) {
    // continue;
    // }
    // members.add(removeMember);
    // }
    //
    // members.removeAll(removeMembers);
    //
    // b.clearMember();
    // b.addallMembers(members);
    //
    // return imageRepository.getImages().update(b);
    // }

    @Override
    public BlobData getImageBlob(Image i) throws IOException {
        ImageImpl image = (ImageImpl) i;
        ImageData data = image.getData();

        Project project = new Project(data.getOwnerProject());
        BlobData imageData = findImageData(project, data);
        return imageData;
    }

    @Override
    public ImageImpl createImage(long projectId, Map<String, String> metadata) throws CloudException {
        ImageData.Builder b = ImageData.newBuilder();
        b.setIsPublic(false);

        if (metadata != null) {
            mapHeadersToBuilder(metadata, b);
        }

        long t = Clock.getTimestamp();
        b.setCreatedAt(t);
        b.setUpdatedAt(t);

        b.setImageState(ImageState.QUEUED);

        if (projectId != 0) {
            b.setOwnerProject(projectId);
        }

        ImageData created = imageRepository.getImages().create(b);
        return new ImageImpl(created);
    }

    @Override
    public ImageImpl uploadData(Image i, BlobData src) throws IOException, CloudException {
        ImageImpl image = (ImageImpl) i;
        if (image == null || src == null) {
            throw new IllegalArgumentException();
        }

        long size = src.size();

        ImageData existing = imageRepository.getImages().find(image.data.getId());
        if (existing == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        ImageData.Builder b = ImageData.newBuilder(existing);

        if (b.hasImageSize()) {
            if (b.getImageSize() != size) {
                throw new IllegalArgumentException();
            }
        }

        b.setUpdatedAt(Clock.getTimestamp());
        b.setImageSize(size);
        b.setImageState(ImageState.ACTIVE);

        {
            String stored = imageDataService.storeImageFile(src);

            ImageLocation.Builder loc = b.getLocationBuilder();
            loc.setStored(stored);

            b.setImageChecksum(src.getHash());
        }

        ImageData updated = imageRepository.getImages().update(b);

        return new ImageImpl(updated);
    }

    public static void mapHeadersToBuilder(Map<String, String> properties, ImageData.Builder b) {
        for (Entry<String, String> property : properties.entrySet()) {
            String key = property.getKey();
            String value = property.getValue();

            if (key.startsWith("property-")) {
                key = key.substring(9);

                KeyValueData.Builder kv = b.getAttributesBuilder().addUserAttributesBuilder();
                kv.setKey(key);
                kv.setValue(value);
            } else if (key.equals(ImageService.METADATA_KEY_NAME)) {
                b.setName(value);
            } else if (key.equals(ImageService.METADATA_KEY_CONTAINER_FORMAT)) {
                b.setContainerFormat(value);
            } else if (key.equals(ImageService.METADATA_KEY_DISK_FORMAT)) {
                b.setDiskFormat(value);
            } else if (key.equals(ImageService.METADATA_KEY_SIZE)) {
                b.setImageSize(Long.valueOf(value));
            } else if (key.equals("is_public")) {
                boolean isPublic = Boolean.parseBoolean(value);
                b.setIsPublic(isPublic);
            } else if (key.equals("protected")) {
                boolean isProtected = Boolean.parseBoolean(value);
                b.setIsProtected(isProtected);
            } else {
                log.warn("Ignoring unknown property: " + key);
            }
        }
    }

    public static class ImageImpl implements ImageService.Image {
        final ImageData data;

        public ImageImpl(ImageData data) {
            this.data = data;
        }

        @Override
        public long getId() {
            return data.getId();
        }

        @Override
        public ImageKey getUniqueKey() {
            return new ImageKey(BaseEncoding.base16().encode(data.getImageChecksum().toByteArray()));
        }

        public ImageData getData() {
            return data;
        }

        @Override
        public String getName() {
            return data.getName();
        }

        @Override
        public String getStatus() {
            return data.getImageState().name();
        }

        @Override
        public String getChecksum() {
            return BaseEncoding.base16().encode(data.getImageChecksum().toByteArray());
        }
    }

    @Override
    public String getUrl(HttpServletRequest httpRequest, long imageId) {
        String url = Urls.getRequestUrl(httpRequest);

        if (!url.endsWith("/")) {
            url += "/";
        }

        url += "openstack/images/v1/images/" + imageId;

        return url;
    }

}
