package io.fathom.cloud.identity.api.os.model.v2;

import io.fathom.cloud.DebugFormatter;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class V2Service {
    @XmlAttribute
    public String type;

    @XmlAttribute
    public String name;

    @XmlElement(name = "endpoints")
    public List<V2Endpoint> endpoints;

    @Override
    public String toString() {
        return DebugFormatter.format(this);
    }

}
