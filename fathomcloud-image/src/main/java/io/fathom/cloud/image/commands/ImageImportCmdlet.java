package io.fathom.cloud.image.commands;

import io.fathom.cloud.commands.Cmdlet;
import io.fathom.cloud.services.ImageImports;
import io.fathom.cloud.services.ImageService.Image;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageImportCmdlet extends Cmdlet {
    private static final Logger log = LoggerFactory.getLogger(ImageImportCmdlet.class);

    @Option(name = "-url", usage = "Image URL", required = true)
    public String imageUrl = null;

    @Inject
    protected ImageImports imageImports;

    public ImageImportCmdlet() {
        super("image-import");
    }

    @Override
    public void run() throws Exception {
        long projectId = 0;
        Image image = imageImports.importImage(projectId, imageUrl);
        log.info("Created image: " + image.getId());
    }

}
