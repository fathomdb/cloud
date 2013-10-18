package io.fathom.cloud.image;

import io.fathom.cloud.image.api.os.resources.BrokenClientsFilter;
import io.fathom.cloud.image.api.os.resources.ImagesV1Endpoint;
import io.fathom.cloud.image.imports.ImageImportsImpl;
import io.fathom.cloud.services.ImageImports;
import io.fathom.cloud.services.ImageService;

import com.fathomdb.extensions.ExtensionModuleBase;
import com.fathomdb.extensions.HttpConfiguration;

public class ImageExtension extends ExtensionModuleBase {

    @Override
    public void addHttpExtensions(HttpConfiguration http) {
        http.bind(ImagesV1Endpoint.class);
        // Don't support the images V2 endpoint until we can find a user of it
        // to test with!
        // http.bind(ImagesV2Endpoint.class);

        // http.filter("/*").through(BrokenClientsFilter.class);
    }

    @Override
    protected void configure() {
        bind(BrokenClientsFilter.class);

        bind(ImageService.class).to(ImageServiceImpl.class);
        bind(ImageImports.class).to(ImageImportsImpl.class);
    }

}
