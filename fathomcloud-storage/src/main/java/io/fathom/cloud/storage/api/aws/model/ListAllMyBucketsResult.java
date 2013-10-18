package io.fathom.cloud.storage.api.aws.model;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ListAllMyBucketsResult")
public class ListAllMyBucketsResult {
    @XmlElement(name = "Owner")
    public Owner owner;

    @XmlElementWrapper(name = "Buckets")
    @XmlElement(name = "Bucket")
    public ArrayList<BucketInfo> buckets;

}
