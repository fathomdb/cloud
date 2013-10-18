package io.fathom.cloud.identity.api.os.resources.extensions;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ClientApp {
    public String id;
    public String name;
    public String secret;
}
