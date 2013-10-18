package io.fathom.cloud.server.resources;

import javax.servlet.http.HttpServletRequest;

public class Urls {

    public static String getRequestUrl(HttpServletRequest httpRequest) {
        String scheme = httpRequest.getScheme(); // http
        int serverPort = httpRequest.getServerPort(); // 80

        String forwardedProtocol = httpRequest.getHeader("X-Forwarded-Protocol");
        if (forwardedProtocol != null) {
            // TODO: Do we always want to enable this??
            scheme = forwardedProtocol;
            if (scheme.equals("https")) {
                serverPort = 443;
            } else if (scheme.equals("http")) {
                serverPort = 80;
            }
        }

        String serverName = httpRequest.getServerName(); // hostname.com
        String contextPath = httpRequest.getContextPath(); // /mywebapp
        // String servletPath = httpRequest.getServletPath(); //
        // /servlet/MyServlet
        // String pathInfo = httpRequest.getPathInfo(); // /a/b;c=123
        // String queryString = httpRequest.getQueryString(); // d=789

        // Reconstruct original requesting URL
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        boolean includePort = true;
        if (serverPort == 80 && scheme.equals("http")) {
            includePort = false;
        } else if (serverPort == 443 && scheme.equals("https")) {
            includePort = false;
        }

        if (includePort) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath);

        // url.append(servletPath);
        //
        // if (pathInfo != null) {
        // url.append(pathInfo);
        // }
        // if (queryString != null) {
        // url.append("?").append(queryString);
        // }
        return url.toString();
    }

}
