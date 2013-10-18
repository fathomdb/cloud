package io.fathom.cloud.image.api.os.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;

public class Image {
    public String id;

    public String uri;
    public String name;

    @XmlElement(name = "disk_format")
    public String diskFormat;

    @XmlElement(name = "container_format")
    public String containerFormat;

    public long size;

    public String checksum;

    public Date createdAt;

    public Date updatedAt;

    public Date deletedAt;

    public String status;

    @XmlElement(name = "is_public")
    public boolean isPublic;

    public String visibility;

    @XmlElement(name = "min_ram")
    public int minimumRam;
    @XmlElement(name = "min_disk")
    public int minimumDisk;

    public String owner;

    public Map<String, String> properties;

    public List<String> tags;

    @XmlElement(name = "protected")
    public Boolean isProtected;

    // HATEOS
    public String self;
    public String file;
    public String schema;

}
