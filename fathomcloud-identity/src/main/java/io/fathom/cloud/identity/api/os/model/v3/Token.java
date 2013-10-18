package io.fathom.cloud.identity.api.os.model.v3;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class Token {
    @XmlAttribute
    public String id;

    @XmlElement(name = "expires_at")
    public Date expires;
    @XmlElement(name = "issued_at")
    public Date issues;

    public List<String> methods;

    public UserInfo user;

    @XmlElement(name = "project")
    public ProjectInfo projectScope;

    @XmlElement(name = "domain")
    public Domain domainScope;

    public List<RoleInfo> roles;

    @XmlElement(name = "catalog")
    public List<Service> serviceCatalog;

    public static class UserInfo {
        public String id;
        public String name;
        public Domain domain;
        public List<Link> links;
    }

    public static class ProjectInfo {
        public String id;
        public String name;
        public Domain domain;
        public List<Link> links;
    }

    public static class RoleInfo {
        public String id;
        public String name;
        public List<Link> links;
    }

}
