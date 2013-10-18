package io.fathom.cloud.image.api.os.resources;

import io.fathom.cloud.protobuf.ImageModel.ImageData;

public class ImageFilter {
    public String name;

    public boolean matches(ImageData image) {
        if (name != null) {
            if (!name.equals(image.getName())) {
                return false;
            }
        }

        return true;
    }
}
