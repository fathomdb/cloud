package io.fathom.cloud.identity.api.os.model.v3;

//import java.io.IOException;
//
//import javax.inject.Singleton;
//import javax.ws.rs.container.ContainerRequestContext;
//import javax.ws.rs.container.ContainerRequestFilter;
//import javax.ws.rs.core.MultivaluedMap;
//import javax.ws.rs.core.UriInfo;
//import javax.ws.rs.ext.Provider;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//@Singleton
//@Provider
//public class DefaultContentTypeFilter implements ContainerRequestFilter {
//    private static final Logger log = LoggerFactory.getLogger(DefaultContentTypeFilter.class);
//
//    @Override
//    public void filter(ContainerRequestContext requestContext) throws IOException {
//        UriInfo uriInfo = requestContext.getUriInfo();
//        String path = uriInfo.getPath();
//        if (path.contains("/identity/")) {
//            MultivaluedMap<String, String> headers = requestContext.getHeaders();
//            if (!headers.containsKey("Accept")) {
//                String accept = "application/json";
//                headers.add("Accept", accept);
//            }
//        }
//    }
//
// }
