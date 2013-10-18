package io.fathom.cloud.identity.api.os.model.v3;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AuthRequest {
    @XmlElement(name = "auth")
    public AuthList authList;

    public static class AuthList {
        public List<String> methods;

        public PasswordAuth password;
        public TokenAuth token;
    }

    public static class PasswordAuth {
        public PasswordAuthUser user;
    }

    public static class PasswordAuthUser {
        @XmlElement(name = "id")
        public String userId;
        @XmlElement(name = "name")
        public String userName;
        public Domain domain;
        public String password;
    }

    public static class TokenAuth {
        public String id;
    }

    public static class ChallengeAuth {
        public String challenge;
        public String response;
    }
}
