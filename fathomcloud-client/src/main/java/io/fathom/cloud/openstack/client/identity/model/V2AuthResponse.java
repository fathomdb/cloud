package io.fathom.cloud.openstack.client.identity.model;

import io.fathom.cloud.openstack.client.SimpleRestClient;

import java.util.Date;
import java.util.List;

public class V2AuthResponse {
    public Access access;

    public String challenge;

    public static class Access {
        public V2Token token;
        public User user;

        public List<Service> serviceCatalog;
    }

    public static class User {
        public String id;

        public String name;

        public Boolean enabled;

        public String email;

        public String description;
    }

    public static class V2Token {
        public String id;

        public Date expires;

        public Tenant tenant;
    }

    public static class Tenant {
        public String id;

        public String name;
    }

    public static class Service {
        public String type;

        public String name;

        public List<Endpoint> endpoints;
    }

    public static class Endpoint {
        public String tenantId;

        public String region;

        public String publicURL;

        public String internalURL;

        public EndpointVersion version;

        public String adminURL;

    }

    public static class EndpointVersion {
        public String id;

        public String info;

        public String list;
    }

    @Override
    public String toString() {
        return SimpleRestClient.asJson(this);
    }

}
