package io.fathom.cloud.compute.api.aws.ec2.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ImportKeyPairResponse", namespace = "http://ec2.amazonaws.com/doc/2012-12-01/")
public class ImportKeyPairResponse {
    public String requestId;

    public String keyName;
    public String keyFingerprint;
}
