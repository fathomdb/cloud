package io.fathom.cloud.identity.api.os.model.v2;

import io.fathom.cloud.Extension;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class V2AuthResponse {
    public Access access;

    @Extension
    public String challenge;

    @Override
    public String toString() {
        return "V2AuthResponse [access=" + access + "]";
    }

}
