package io.fathom.cloud.openstack.client.identity.model;

public class V2AuthRequest {
    public V2AuthCredentials auth;

    public static class V2AuthCredentials {
        public String tenantName;
        public String tenantId;

        public PasswordCredentials passwordCredentials;

        public ChallengeResponse challengeResponse;

        public TokenCredentials token;

        public class TokenCredentials {
            public String id;
        }
    }

    public static class PasswordCredentials {
        public String username;
        public String password;
    }

    public static class ChallengeResponse {
        public String challenge;
        public String response;
    }

    public static V2AuthRequest create(String username, String password) {
        V2AuthRequest request = new V2AuthRequest();
        request.auth = new V2AuthCredentials();
        request.auth.passwordCredentials = new PasswordCredentials();
        request.auth.passwordCredentials.username = username;
        request.auth.passwordCredentials.password = password;
        return request;
    }

}
