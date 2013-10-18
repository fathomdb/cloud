package io.fathom.cloud.identity.api.os.model.v2;

import io.fathom.cloud.identity.api.os.model.User;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class Access {
    public V2Token token;
    public User user;

    @XmlElement(name = "serviceCatalog")
    public List<V2Service> services;

    @Override
    public String toString() {
        return "Access [token=" + token + ", user=" + user + ", services=" + services + "]";
    }

}
