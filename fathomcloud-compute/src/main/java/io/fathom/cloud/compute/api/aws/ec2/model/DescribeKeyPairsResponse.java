package io.fathom.cloud.compute.api.aws.ec2.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DescribeKeyPairsResponse", namespace = "http://ec2.amazonaws.com/doc/2012-12-01/")
public class DescribeKeyPairsResponse {
    public String requestId;

    @XmlElementWrapper(name = "keySet")
    @XmlElement(name = "item")
    public List<Item> keys;

    public static class Item {
        public String keyName;
        public String keyFingerprint;
    }

}
