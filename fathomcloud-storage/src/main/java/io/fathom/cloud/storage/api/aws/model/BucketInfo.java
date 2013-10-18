package io.fathom.cloud.storage.api.aws.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;

public class BucketInfo {
    @XmlElement(name = "Name")
    public String name;
    @XmlElement(name = "CreationDate")
    public Date creationDate;
}
