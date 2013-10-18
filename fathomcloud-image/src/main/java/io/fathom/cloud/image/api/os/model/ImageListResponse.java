package io.fathom.cloud.image.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ImageListResponse {
    public List<Image> images;
}
