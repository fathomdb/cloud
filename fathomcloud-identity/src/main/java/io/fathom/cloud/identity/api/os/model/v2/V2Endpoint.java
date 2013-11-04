package io.fathom.cloud.identity.api.os.model.v2;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class V2Endpoint {
    @XmlAttribute
    public String tenantId;

    @XmlAttribute
    public String region;

    @XmlAttribute(name = "publicURL")
    public String publicURL;

    @XmlAttribute(name = "internalURL")
    public String internalURL;

    public V2EndpointVersion version;

    @XmlElement(name = "versionId")
    public String versionId;

    @XmlAttribute(name = "adminURL")
    public String adminURL;

}
