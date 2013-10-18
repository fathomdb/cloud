package io.fathom.cloud.image.state;

import io.fathom.cloud.protobuf.ImageModel.ImageData;
import io.fathom.cloud.state.NumberedItemCollection;
import io.fathom.cloud.state.RepositoryBase;
import io.fathom.cloud.state.StateStore.StateNode;

import javax.inject.Singleton;

@Singleton
public class ImageStore extends RepositoryBase {

    public NumberedItemCollection<ImageData> getImages() {
        StateNode root = stateStore.getRoot("images");

        return new NumberedItemCollection<ImageData>(root, ImageData.newBuilder(), ImageData.getDescriptor()
                .findFieldByNumber(ImageData.ID_FIELD_NUMBER));
    }

}
