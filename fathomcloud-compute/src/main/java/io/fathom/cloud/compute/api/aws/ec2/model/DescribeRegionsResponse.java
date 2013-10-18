package io.fathom.cloud.compute.api.aws.ec2.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DescribeRegionsResponse", namespace = "http://ec2.amazonaws.com/doc/2012-12-01/")
public class DescribeRegionsResponse {
    public String requestId;

    public RegionInfo regionInfo;

    public static class RegionInfo {
        @XmlElement(name = "item")
        public List<Item> regions;

        public static class Item {
            public String regionName;
            public String regionEndpoint;
        }
    }

}
