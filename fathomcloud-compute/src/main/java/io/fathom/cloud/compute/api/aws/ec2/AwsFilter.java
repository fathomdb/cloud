package io.fathom.cloud.compute.api.aws.ec2;

import java.io.IOException;

import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

@Singleton
public class AwsFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("AWS ")) {
            String uri = request.getRequestURI();
            if (uri.isEmpty() || uri.charAt(0) != '/') {
                uri = "/" + uri;
            }
            uri = "/aws/s3" + uri;

            req.getRequestDispatcher(uri).forward(req, res);
        } else {
            chain.doFilter(request, res);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }

}
