package io.fathom.cloud.identity.api.os.model.v2;

import javax.xml.bind.annotation.XmlAttribute;

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

    public String versionId;

    @XmlAttribute
    public String adminURL;

}
