package io.fathom.cloud.server.auth;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

@Singleton
public class WebAuthProvider implements AuthProvider {

    @Inject
    Provider<HttpServletRequest> httpRequestProvider;

    @Override
    public TokenAuth get() {
        HttpServletRequest httpServletRequest = httpRequestProvider.get();
        if (httpServletRequest != null) {
            return TokenAuthFilter.findAuth(httpServletRequest);
        }
        return null;
    }

}
