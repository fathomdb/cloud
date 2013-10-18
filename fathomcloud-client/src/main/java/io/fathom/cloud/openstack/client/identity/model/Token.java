package io.fathom.cloud.openstack.client.identity.model;

import java.util.Date;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Token {
    @SerializedName("expires_at")
    public Date expires;
    @SerializedName("issued_at")
    public Date issues;

    @SerializedName("catalog")
    public List<Service> serviceCatalog;
    public List<String> methods;

    public UserInfo user;

    @SerializedName("project")
    public ProjectInfo projectScope;

    @SerializedName("domain")
    public Domain domainScope;

    public List<RoleInfo> roles;

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
