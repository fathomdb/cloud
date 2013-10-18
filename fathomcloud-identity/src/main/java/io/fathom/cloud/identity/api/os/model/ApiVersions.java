package io.fathom.cloud.identity.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class ApiVersions {
    public VersionList versions;

    public static class VersionList {
        @XmlElement(name = "version")
        public List<ApiVersion> versions;

    }
}
