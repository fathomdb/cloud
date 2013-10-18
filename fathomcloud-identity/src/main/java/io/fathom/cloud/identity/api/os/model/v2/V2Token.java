package io.fathom.cloud.identity.api.os.model.v2;

import java.util.Date;

import javax.xml.bind.annotation.XmlAttribute;

public class V2Token {
    @XmlAttribute
    public String id;

    @XmlAttribute
    public Date expires;

    public Tenant tenant;

    @Override
    public String toString() {
        return "V2Token [id=" + id + ", expires=" + expires + ", tenant=" + tenant + "]";
    }

}
