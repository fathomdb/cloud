package io.fathom.cloud.identity.api.os.model;

import javax.xml.bind.annotation.XmlElement;

public class Credential {
    @XmlElement(name = "user_id")
    public String userId;

    @XmlElement(name = "tenant_id")
    public String tenantId;

    public String access;
    public String secret;
}
