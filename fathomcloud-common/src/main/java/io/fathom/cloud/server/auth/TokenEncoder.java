package io.fathom.cloud.server.auth;

import io.fathom.cloud.protobuf.CloudCommons.TokenInfo;

public interface TokenEncoder {
    String encodeToken(TokenInfo tokenInfo);
}
