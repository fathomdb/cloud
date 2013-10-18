package io.fathom.cloud.server.auth;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import io.fathom.cloud.protobuf.CloudCommons.TokenInfo;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class TokenAuthFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(TokenAuthFilter.class);

    static final String AUTH_HEADER = "X-Auth-Token";
    static final String ATTRIBUTE_NAME = TokenAuth.class.getName();

    private final TokenService tokenService;

    @Inject
    public TokenAuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void init(FilterConfig config) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse rsp = (HttpServletResponse) response;

        if (verify(req, rsp)) {
            chain.doFilter(req, rsp);
        }
    }

    private boolean verify(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        final String tokenId = req.getHeader(AUTH_HEADER);
        if (tokenId == null) {
            // Allow an anonymous connection through
            return true;
        }

        TokenInfo tokenInfo = null;
        try {
            tokenInfo = tokenService.findValidToken(tokenId);
        } catch (Exception e) {
            log.warn("Unexpected error while reading token", e);
        }

        if (tokenInfo == null) {
            log.debug("Token could not verified");
            rsp.sendError(SC_UNAUTHORIZED);
            return false;
        }

        TokenAuth auth = new TokenAuth(tokenInfo);
        req.setAttribute(ATTRIBUTE_NAME, auth);
        return true;
    }

    public static TokenAuth findAuth(HttpServletRequest httpServletRequest) {
        return (TokenAuth) httpServletRequest.getAttribute(ATTRIBUTE_NAME);
    }
}
