package io.fathom.cloud.dns.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.dns.backend.DnsBackend;
import io.fathom.cloud.dns.backend.aws.AwsDnsBackend;
import io.fathom.cloud.dns.backend.federated.FederatedDnsBackend;
import io.fathom.cloud.dns.backend.selfhosted.SelfHostedDnsBackend;
import io.fathom.cloud.dns.state.DnsRepository;
import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.identity.CertificateAuthTokenProvider;
import io.fathom.cloud.openstack.client.identity.OpenstackIdentityClient;
import io.fathom.cloud.protobuf.DnsModel.BackendData;
import io.fathom.cloud.protobuf.DnsModel.BackendDataOrBuilder;
import io.fathom.cloud.protobuf.DnsModel.BackendSecretData;
import io.fathom.cloud.protobuf.DnsModel.BackendSecretDataOrBuilder;
import io.fathom.cloud.protobuf.DnsModel.DnsBackendProviderType;
import io.fathom.cloud.ssh.SshContext;
import io.fathom.cloud.ssh.jsch.SshContextImpl;
import io.fathom.cloud.state.DuplicateValueException;

import java.net.URI;
import java.security.KeyPair;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.persist.Transactional;

@Singleton
@Transactional
public class DnsBackends {
    private static final Logger log = LoggerFactory.getLogger(DnsBackends.class);

    @Inject
    DnsSecrets dnsSecrets;

    @Inject
    SshContext sshContext;

    @Inject
    Provider<AwsDnsBackend> awsProvider;

    @Inject
    Provider<FederatedDnsBackend> federatedProvider;

    @Inject
    Provider<SelfHostedDnsBackend> selfHostedProvider;

    @Inject
    DnsRepository repository;

    // @Inject
    // public DnsBackends(Configuration config) {
    // this.defaultBackendProvider = config.lookup("dns.backend",
    // DnsBackendProviderType.SELF_HOSTED);
    // }

    public DnsBackend getBackend(BackendData backendData) {
        switch (backendData.getType()) {
        case AWS_ROUTE53: {
            AwsDnsBackend aws = awsProvider.get();
            aws.init(backendData);
            return aws;
        }

        case OPENSTACK: {
            FederatedDnsBackend federated = federatedProvider.get();
            federated.init(backendData);
            return federated;
        }

        case SELF_HOSTED: {
            SelfHostedDnsBackend selfHosted = selfHostedProvider.get();
            selfHosted.init(backendData);
            return selfHosted;
        }

        default:
            throw new IllegalArgumentException();
        }
    }

    public DnsBackend getGenericBackend() {
        return selfHostedProvider.get();
    }

    public List<BackendData> listBackends() throws CloudException {
        return repository.getBackends().list();
    }

    public BackendData register(BackendData.Builder backend, BackendSecretData.Builder secrets, String email)
            throws CloudException {
        if (repository.getBackends().find(backend.getKey()) != null) {
            throw new WebApplicationException(Status.CONFLICT);
        }

        if (backend.getDefault()) {
            for (BackendData b : repository.getBackends().list()) {
                if (b.getDefault()) {
                    // TODO: Support changing
                    throw new IllegalArgumentException("Default backend already set");
                }
            }
        }

        if (backend.getType() == DnsBackendProviderType.OPENSTACK) {
            registerOpenstackBackend(backend, email);
        }

        if (backend.getType() == DnsBackendProviderType.AWS_ROUTE53) {
            registerAwsRoute53Backend(backend, secrets);
        }

        backend.setSecretData(dnsSecrets.encrypt(secrets.build()));

        BackendData created;
        {
            try {
                created = repository.getBackends().create(backend);
            } catch (DuplicateValueException e) {
                // Shouldn't happen, unless there's concurrent operation
                throw new IllegalStateException("Duplicate provider name while creating registration");
            }
        }
        return created;
    }

    private void registerAwsRoute53Backend(BackendDataOrBuilder backend, BackendSecretDataOrBuilder secrets) {
        if (Strings.isNullOrEmpty(secrets.getUsername()) || Strings.isNullOrEmpty(secrets.getPassword())) {
            throw new IllegalArgumentException("Username and password are required to use AWS Route 53 DNS backend");
        }
    }

    void registerOpenstackBackend(BackendData.Builder backend, String email) throws CloudException {
        KeyPair keypair = ((SshContextImpl) sshContext).getKeypair();
        URI uri = URI.create(backend.getUrl());

        OpenstackIdentityClient identityClient;
        try {
            identityClient = CertificateAuthTokenProvider.ensureRegistered(keypair, uri, email);
        } catch (RestClientException e) {
            throw new CloudException("Error registering with server", e);
        }

        String project;
        try {
            project = identityClient.getUtils().ensureProjectWithPrefix("__federation__");
        } catch (RestClientException e) {
            throw new CloudException("Error creating project", e);
        }

        backend.setBackendCookie(project);
    }
}
