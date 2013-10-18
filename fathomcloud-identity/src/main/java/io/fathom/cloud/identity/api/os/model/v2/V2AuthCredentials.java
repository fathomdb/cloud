package io.fathom.cloud.identity.api.os.model.v2;

import io.fathom.cloud.Extension;

import javax.xml.bind.annotation.XmlElement;

public class V2AuthCredentials {
    public String tenantName;
    public String tenantId;

    public PasswordCredentials passwordCredentials;

    @Extension
    public ChallengeResponse challengeResponse;

    @XmlElement(name = "token")
    public TokenCredentials tokenCredentials;

    public class TokenCredentials {
        public String id;

        @Override
        public String toString() {
            return "TokenCredentials [id=" + id + "]";
        }

    }

    @Override
    public String toString() {
        return "V2AuthCredentials [tenantName=" + tenantName + ", tenantId=" + tenantId + ", passwordCredentials="
                + passwordCredentials + ", tokenCredentials=" + tokenCredentials + "]";
    }

}