package io.fathom.auto.haproxy;

import io.fathom.auto.JsonCodec;
import io.fathom.auto.config.SecretKeys;
import io.fathom.auto.haproxy.model.Backend;
import io.fathom.auto.haproxy.model.Frontend;
import io.fathom.auto.haproxy.template.BackendTemplate;
import io.fathom.auto.haproxy.template.FrontendTemplate;
import io.fathom.auto.haproxy.template.HeaderTemplate;
import io.fathom.cloud.openstack.client.loadbalance.model.LbaasMapping;
import io.fathom.cloud.openstack.client.loadbalance.model.LoadBalanceMappingList;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

public class HaproxyConfigBuilder {
    private static final String DEFAULT_FRONTEND_KEY = "_default";
    private static final String DEFAULT_HOST_KEY = "_default";

    private static final Logger log = LoggerFactory.getLogger(HaproxyConfigBuilder.class);

    private final File keysDir;
    private final File mirrorDir;
    private final SecretKeys secretKeys;
    private String defaultHost;

    public HaproxyConfigBuilder(File mirrorDir, File keysDir, SecretKeys secretKeys) {
        this.mirrorDir = mirrorDir;
        this.keysDir = keysDir;
        this.secretKeys = secretKeys;
    }

    public void visitDir(File dir) throws IOException {
        for (File file : dir.listFiles()) {
            if (!file.isFile()) {
                log.warn("Expected file: {}", file);
                continue;
            }
            visitFile(file);
        }
    }

    private void visitFile(File file) throws IOException {
        String json = Files.toString(file, Charsets.UTF_8);

        log.debug("Reading file: {}", file);

        LoadBalanceMappingList chunk = JsonCodec.gson.fromJson(json, LoadBalanceMappingList.class);

        if (chunk.mappings != null) {
            for (LbaasMapping mapping : chunk.mappings) {
                log.debug("Found mapping: {}", mapping);

                Backend backend = buildBackend(mapping);

                backend.mappings.add(mapping);
            }
        }

    }

    private Backend buildBackend(LbaasMapping mapping) throws IOException {
        String host = mapping.host;
        String key;
        if (host == null) {
            key = DEFAULT_HOST_KEY;
        } else {
            key = buildKey(host);
        }

        Backend backend = backends.get(key);
        if (backend != null) {
            return backend;
        }

        backend = new Backend();
        backend.host = host;
        backend.key = key;
        backends.put(backend.key, backend);

        SecretKeys.SecretInfo secret;
        if (host != null) {
            secret = secretKeys.findSecret(host);
        } else if (defaultHost != null) {
            log.info("Checking secret for default host: " + defaultHost);
            secret = secretKeys.findSecret(defaultHost);
        } else {
            secret = null;
        }

        if (secret != null) {
            // TODO: Check if secret changed??
            String s = secret.read();
            File secretFile = new File(keysDir, secret.getId());
            Files.write(s, secretFile, Charsets.UTF_8);
            backend.sslKey = secretFile.getAbsolutePath();
        }

        String frontendKey = DEFAULT_FRONTEND_KEY;
        Frontend frontend = getFrontend(frontendKey);
        frontend.backends.add(backend);

        if (backend.sslKey != null) {
            if (frontend.sslKey == null) {
                frontend.sslKey = backend.sslKey;
            } else if (!Objects.equal(frontend.sslKey, backend.sslKey)) {
                log.warn("Arbitrarily choosing frontend sslKey");
            }
        }
        return backend;
    }

    private Frontend getFrontend(String frontendKey) {
        Frontend frontend = frontends.get(frontendKey);
        if (frontend == null) {
            frontend = new Frontend();
            frontend.key = frontendKey;
            frontends.put(frontendKey, frontend);
        }
        return frontend;
    }

    public File getMirrorDir() {
        return mirrorDir;
    }

    final Map<String, Backend> backends = Maps.newHashMap();

    final Map<String, Frontend> frontends = Maps.newHashMap();

    static String buildKey(String s) {
        String escaped;
        try {
            escaped = URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Cannot find UTF-8 encoding", e);
        }
        // escaped = escaped.replace('%', '_');
        return escaped;
    }

    public void generateConfig(Writer writer) throws IOException {
        if (frontends.size() > 1) {
            throw new IllegalStateException();
        }

        if (frontends.size() == 0) {
            getFrontend(DEFAULT_FRONTEND_KEY);
        }

        FrontendTemplate frontendTemplate = new FrontendTemplate();
        BackendTemplate backendTemplate = new BackendTemplate();
        HeaderTemplate headerTemplate = new HeaderTemplate();

        headerTemplate.write(writer, null);

        for (Frontend frontend : frontends.values()) {
            frontendTemplate.write(writer, frontend);
        }

        for (Backend backend : backends.values()) {
            backendTemplate.write(writer, backend);
        }
    }

    public String getDefaultHost() {
        return defaultHost;
    }

    public void setDefaultHost(String defaultHost) {
        this.defaultHost = defaultHost;
    }

}
