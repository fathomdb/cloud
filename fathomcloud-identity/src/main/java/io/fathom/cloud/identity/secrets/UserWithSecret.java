package io.fathom.cloud.identity.secrets;

import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.protobuf.IdentityModel.UserSecretData;

public class UserWithSecret {
    final UserData userData;
    final UserSecretData userSecretData;
    final SecretToken secretToken;

    public UserWithSecret(UserData userData, UserSecretData userSecretData, SecretToken secretToken) {
        this.userData = userData;
        this.userSecretData = userSecretData;
        this.secretToken = secretToken;
    }

    public UserData getUserData() {
        return userData;
    }

    public SecretToken getSecretToken() {
        return secretToken;
    }

}