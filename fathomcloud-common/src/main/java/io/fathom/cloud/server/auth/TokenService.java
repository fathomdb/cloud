package io.fathom.cloud.server.auth;

import io.fathom.cloud.protobuf.CloudCommons.TokenInfo;

public interface TokenService {
    TokenInfo findValidToken(String tokenId);
}
