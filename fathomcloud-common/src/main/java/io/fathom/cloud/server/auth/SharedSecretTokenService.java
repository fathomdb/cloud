package io.fathom.cloud.server.auth;

import io.fathom.cloud.protobuf.CloudCommons.Token;
import io.fathom.cloud.protobuf.CloudCommons.TokenInfo;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.keyczar.Signer;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.util.Base64Coder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;

@Singleton
public class SharedSecretTokenService implements TokenService, TokenEncoder {
    private static final Logger log = LoggerFactory.getLogger(SharedSecretTokenService.class);

    public static final String KEYSTORE_ID = "token_sign";

    @Inject
    SharedKeystore keystore;

    private Signer signer;

    @Override
    public TokenInfo findValidToken(String tokenString) {
        if (Strings.isNullOrEmpty(tokenString)) {
            return null;
        }

        try {
            byte[] tokenBytes = Base64Coder.decodeWebSafe(tokenString);

            Token token = Token.parseFrom(tokenBytes);

            TokenInfo tokenInfo = token.getTokenInfo();
            byte[] plaintext = tokenInfo.toByteArray();
            if (!getSigner().verify(plaintext, token.getSignature().toByteArray())) {
                log.debug("Token signature verification failed");
                return null;
            }

            if (TokenAuth.hasExpired(tokenInfo)) {
                log.debug("Token has expired");
                return null;
            }

            return tokenInfo;
        } catch (Exception e) {
            log.debug("Error during token validation", e);
            return null;
        }
    }

    @Override
    public String encodeToken(TokenInfo tokenInfo) {
        Token.Builder token = Token.newBuilder();
        token.setTokenInfo(tokenInfo);

        byte[] plaintext = tokenInfo.toByteArray();
        byte[] signature;
        try {
            signature = getSigner().sign(plaintext);
        } catch (KeyczarException e) {
            throw new IllegalStateException("Error signing token", e);
        }

        log.info("Token plaintext size {} signature size {}", plaintext.length, signature.length);
        token.setSignature(ByteString.copyFrom(signature));

        String encoded = Base64Coder.encodeWebSafe(token.build().toByteArray());
        log.info("Token encoded size {}", encoded.length());
        return encoded;
    }

    private synchronized Signer getSigner() throws KeyczarException {
        if (signer == null) {
            this.signer = keystore.buildSigner(KEYSTORE_ID);
        }

        return signer;
    }

}
