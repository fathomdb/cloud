package io.fathom.cloud.compute.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class VolumeAttachments {
    @XmlElement(name = "volumeAttachments")
    public List<VolumeAttachment> volumeAttachments;
}
