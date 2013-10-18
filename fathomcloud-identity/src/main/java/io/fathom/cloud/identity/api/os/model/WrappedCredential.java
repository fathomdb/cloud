package io.fathom.cloud.identity.api.os.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class WrappedCredential {
    public Credential credential;
}
