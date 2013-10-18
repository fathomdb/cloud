package io.fathom.cloud.image.api.os.model;

import javax.xml.bind.annotation.XmlElement;

public class WrappedImage {
    @XmlElement(name = "image")
    public Image image;
}
