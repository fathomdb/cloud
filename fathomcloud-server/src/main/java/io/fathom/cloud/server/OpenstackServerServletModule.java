package io.fathom.cloud.server;

import io.fathom.cloud.jaxrs.JaxrsServletModule;
import io.fathom.cloud.server.auth.TokenAuthFilter;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.wink.common.internal.registry.InjectableFactory;
import org.apache.wink.guice.InjectedRestServlet;
import org.apache.wink.server.internal.registry.ServerInjectableFactory;
import org.eclipse.jetty.servlets.GzipFilter;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.fathomdb.Configuration;
import com.fathomdb.extensions.Extensions;
import com.fathomdb.extensions.HttpConfiguration;
import com.google.inject.Scopes;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.persist.PersistFilter;

public class OpenstackServerServletModule extends JaxrsServletModule {
    // extends JerseyServletModule {
    private final Extensions extensions;
    private final Configuration configuration;

    public OpenstackServerServletModule(Configuration configuration, Extensions extensions) {
        this.configuration = configuration;
        this.extensions = extensions;
    }

    @Override
    protected void configureServlets() {
        extensions.addHttpExtensions(new HttpConfiguration() {
            @Override
            public FilterKeyBindingBuilder filter(String urlPattern) {
                return OpenstackServerServletModule.this.filter(urlPattern);
            }

            @Override
            public ServletKeyBindingBuilder serve(String urlPattern) {
                return OpenstackServerServletModule.this.serve(urlPattern);
            }

            @Override
            public <T> AnnotatedBindingBuilder<T> bind(Class<T> clazz) {
                return OpenstackServerServletModule.this.bind(clazz);
            }
        });

        bind(Extensions.class).toInstance(extensions);

        if (configuration.lookup("http.cors.enabled", false)) {
            throw new UnsupportedOperationException();
            // bind(CORSFilter.class).asEagerSingleton();
            // filter("/api/*").through(CORSFilter.class);
        }

        boolean USE_GZIP = true;
        if (USE_GZIP) {
            bind(GzipFilter.class).in(Scopes.SINGLETON);

            Map<String, String> params = new HashMap<String, String>();
            params.put("mimeType",
                    "text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/javascript,image/svg+xml,application/json");
            filter("/*").through(GzipFilter.class, params);
        }

        // boolean ENABLE_EC2 = false;
        // if (ENABLE_EC2) {
        // bind(Ec2Endpoint.class);
        // }
        // boolean ENABLE_S3 = false;
        // if (ENABLE_S3) {
        // bind(S3Resource.class);
        // }
        //
        // if (ENABLE_S3 || ENABLE_EC2) {
        // filter("/*").through(AwsFilter.class);
        // }

        // Configure Jackson for JSON output
        {
            ObjectMapper objectMapper = new ObjectMapper();
            // Include always because horizon etc are really fussy
            // objectMapper.setSerializationInclusion(Include.NON_NULL);
            objectMapper.setSerializationInclusion(Include.ALWAYS);

            // Use JAXB annotations
            AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
            objectMapper = objectMapper.setAnnotationIntrospector(introspector);

            // mapper = mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES,
            // true);
            // mapper =
            // mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES,
            // true);
            // mapper = mapper.configure(JsonParser.Feature.ALLOW_COMMENTS,
            // true);

            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

            objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SZ"));

            JacksonJsonProvider jacksonJsonProvider = new JacksonJsonProvider(objectMapper);
            bind(JacksonJsonProvider.class).toInstance(jacksonJsonProvider);
        }

        bind(GsonObjectMessageBodyHandler.class);

        // filter("/api/*").through(CORSFilter.class);

        // filter("/api/*").through(AuthenticationFilter.class);

        // {
        // Map<String, String> params = new HashMap<String, String>();
        // bind(DefaultServlet.class).in(Scopes.SINGLETON);
        //
        // params.put("dirAllowed", "false");
        // params.put("gzip", "true");
        //
        // URL urlStatic = getClass().getResource("/webapp");
        // params.put("resourceBase", urlStatic.toString());
        //
        // serve("/static/*").with(DefaultServlet.class, params);
        // }

        filter("/*").through(TokenAuthFilter.class);

        filter("/*").through(PersistFilter.class);

        // {
        // bind(JerseyBridgeRequestFilter.class);
        //
        // Map<String, String> params = new HashMap<String, String>();
        // params.put(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS,
        // JerseyBridgeRequestFilter.class.getName());
        // // GZIPContentEncodingFilter.class.getName()
        // // + "," + StripExtensionFilter.class.getName() + "," +
        // // MetadataFilter.class.getName());
        // // if (USE_GZIP) {
        // // params.put(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS,
        // // GZIPContentEncodingFilter.class.getName());
        // // }
        //
        // serve("/*").with(GuiceContainer.class, params);
        // }

        {
            Map<String, String> params = new HashMap<String, String>();
            // params.put(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS,
            // JerseyBridgeRequestFilter.class.getName());
            // GZIPContentEncodingFilter.class.getName()
            // + "," + StripExtensionFilter.class.getName() + "," +
            // MetadataFilter.class.getName());
            // if (USE_GZIP) {
            // params.put(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS,
            // GZIPContentEncodingFilter.class.getName());
            // }

            // params.put(GuiceRestServlet.DEPLOYMENT_CONF_PARAM,
            // InjectorDeploymentConfiguration.class.getName());
            // bind(GuiceRestServlet.class).asEagerSingleton();
            // serve("/*").with(GuiceRestServlet.class, params);

            // TODO: WHY???
            InjectableFactory.setInstance(new ServerInjectableFactory());

            serve("/*").with(InjectedRestServlet.class, params);
        }

        // {
        //
        // Map<String, String> params = new HashMap<String, String>();
        // // params.put(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS,
        // // GZIPContentEncodingFilter.class.getName()
        // // + "," + StripExtensionFilter.class.getName() + "," +
        // // MetadataFilter.class.getName());
        // // if (USE_GZIP) {
        // // params.put(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS,
        // // GZIPContentEncodingFilter.class.getName());
        // // }
        //
        // bind(HttpServletDispatcher.class).in(Scopes.SINGLETON);
        //
        // serve("/*").with(HttpServletDispatcher.class, params);
        // }
    }
}
