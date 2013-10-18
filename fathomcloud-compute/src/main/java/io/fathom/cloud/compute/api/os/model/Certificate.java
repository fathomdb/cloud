package io.fathom.cloud.compute.api.os.model;

import javax.xml.bind.annotation.XmlElement;

public class Certificate {

    public String data;

    @XmlElement(name = "private_key")
    public String privateKey;

}
