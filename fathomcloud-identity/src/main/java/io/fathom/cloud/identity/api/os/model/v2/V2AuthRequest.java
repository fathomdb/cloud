package io.fathom.cloud.identity.api.os.model.v2;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class V2AuthRequest {
    @XmlElement(name = "auth")
    public V2AuthCredentials authInfo;
}
