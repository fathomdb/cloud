package io.fathom.cloud.compute.api.aws.ec2;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.aws.ec2.actions.AwsAction;
import io.fathom.cloud.compute.api.aws.ec2.actions.AwsActionHandler;
import io.fathom.cloud.compute.api.aws.ec2.actions.DeleteKeyPair;
import io.fathom.cloud.compute.api.aws.ec2.actions.DescribeInstances;
import io.fathom.cloud.compute.api.aws.ec2.actions.DescribeKeyPairs;
import io.fathom.cloud.compute.api.aws.ec2.actions.DescribeRegions;
import io.fathom.cloud.compute.api.aws.ec2.actions.ImportKeyPair;
import io.fathom.cloud.compute.api.aws.ec2.actions.RunInstancesHandler;
import io.fathom.cloud.compute.api.aws.ec2.actions.TerminateInstances;
import io.fathom.cloud.server.resources.FathomCloudResourceBase;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.inject.Injector;

@Path("/")
public class Ec2Endpoint extends FathomCloudResourceBase {
    private static final Logger log = LoggerFactory.getLogger(Ec2Endpoint.class);

    @Inject
    Injector injector;

    @Inject
    Provider<AwsRequestContext> requestContextProvider;

    @POST
    @Produces({ XML })
    @Consumes("application/x-www-form-urlencoded")
    public Object doPost(final MultivaluedMap<String, String> formParameters) throws CloudException {
        for (Entry<String, List<String>> entry : formParameters.entrySet()) {
            log.info(entry.getKey() + "=" + entry.getValue());
        }

        String action = formParameters.getFirst("Action");
        if (action == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Map<String, Class<?>> handlers = buildHandlers();

        Class<?> handlerClass = handlers.get(action);
        if (handlerClass == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        AwsActionHandler handler = (AwsActionHandler) injector.getInstance(handlerClass);

        AwsRequestContext requestContext = requestContextProvider.get();
        handler.init(requestContext, formParameters);

        return handler.go();

    }

    private static Map<String, Class<?>> buildHandlers() {
        // TODO: We could do this using discovery
        Map<String, Class<?>> handlers = Maps.newHashMap();
        addHandler(handlers, DescribeRegions.class);
        addHandler(handlers, ImportKeyPair.class);
        addHandler(handlers, DescribeKeyPairs.class);
        addHandler(handlers, DeleteKeyPair.class);
        addHandler(handlers, DescribeInstances.class);
        addHandler(handlers, RunInstancesHandler.class);
        addHandler(handlers, TerminateInstances.class);
        return handlers;
    }

    private static void addHandler(Map<String, Class<?>> handlers, Class<? extends AwsActionHandler> clazz) {
        AwsAction action = clazz.getAnnotation(AwsAction.class);
        if (action == null) {
            throw new IllegalStateException("No @Action annotation on: " + clazz);
        }
        handlers.put(action.value(), clazz);
    }

}
