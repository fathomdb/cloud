package io.fathom.cloud.compute.api.aws.ec2.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DeleteKeyPairResponse", namespace = "http://ec2.amazonaws.com/doc/2012-12-01/")
public class DeleteKeyPairResponse {
    public String requestId;

    @XmlElement(name = "return")
    public boolean returnValue;

}
