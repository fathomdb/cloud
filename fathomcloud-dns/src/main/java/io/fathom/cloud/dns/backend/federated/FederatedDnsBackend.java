package io.fathom.cloud.dns.backend.federated;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.dns.backend.DnsBackendBase;
import io.fathom.cloud.dns.model.DnsZone;
import io.fathom.cloud.openstack.client.OpenstackClient;
import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.dns.OpenstackDnsClient;
import io.fathom.cloud.openstack.client.dns.model.Recordset;
import io.fathom.cloud.openstack.client.dns.model.Zone;
import io.fathom.cloud.openstack.client.identity.CertificateAuthTokenProvider;
import io.fathom.cloud.openstack.client.identity.ChallengeResponses;
import io.fathom.cloud.openstack.client.identity.OpenstackIdentityClient;
import io.fathom.cloud.protobuf.DnsModel.BackendData;
import io.fathom.cloud.protobuf.DnsModel.DnsBackendProviderType;
import io.fathom.cloud.protobuf.DnsModel.DnsSuffixData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.ssh.SshContext;
import io.fathom.cloud.ssh.jsch.SshContextImpl;
import io.fathom.cloud.state.DuplicateValueException;
import io.fathom.cloud.tasks.TaskScheduler;
import io.fathom.http.HttpClient;
import io.fathom.http.jre.JreHttpClient;

import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.x500.X500Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.crypto.CertificateAndKey;

@Singleton
public class FederatedDnsBackend extends DnsBackendBase {
    private static final Logger log = LoggerFactory.getLogger(FederatedDnsBackend.class);

    @Inject
    TaskScheduler taskScheduler;

    @Inject
    SshContext sshContext;

    private BackendData backendData;

    OpenstackClient getOpenstackClient() {
        KeyPair keypair = getKeyPair();

        URI uri = URI.create(backendData.getUrl());

        HttpClient httpClient = JreHttpClient.create();
        OpenstackIdentityClient identityClient = new OpenstackIdentityClient(httpClient, uri, null);

        X500Principal subject = new X500Principal("CN=" + "unknown");
        CertificateAndKey certificateAndKey = ChallengeResponses.createSelfSigned(subject, keypair);

        String project = backendData.getBackendCookie();
        if (project == null) {
            throw new IllegalStateException();
            // log.warn("No backend project configured: {}",
            // backendData);
            // project = "__federation__";
        }

        CertificateAuthTokenProvider tokenProvider = new CertificateAuthTokenProvider(identityClient, project,
                certificateAndKey);

        OpenstackClient openstackClient = OpenstackClient.build(tokenProvider);
        return openstackClient;
    }

    KeyPair getKeyPair() {
        KeyPair keypair = ((SshContextImpl) sshContext).getKeypair();
        return keypair;
    }

    public void init(BackendData backendData) {
        this.backendData = backendData;
    }

    @Override
    public void updateDomain(Project project, DnsZone domain) {
        UpdateFederated job = new UpdateFederated(project, domain);

        taskScheduler.execute(job);
    }

    @Override
    public String createZone(Project project, String zone, String topZone, DnsSuffixData suffixData)
            throws CloudException {
        try {
            OpenstackClient openstackClient = getOpenstackClient();
            OpenstackDnsClient dns = openstackClient.getDns();

            Zone request = new Zone();
            request.name = zone;

            Zone response = dns.createZone(request);
            return response.id;
        } catch (RestClientException e) {
            if (e.is(409)) {
                throw new DuplicateValueException();
            }
            throw new CloudException("Error creating zone", e);
        }
    }

    @Override
    public DnsBackendProviderType getType() {
        return DnsBackendProviderType.OPENSTACK;
    }

    public class UpdateFederated extends UpdateDnsDomainBase {
        protected final OpenstackClient openstackClient;

        public UpdateFederated(Project project, DnsZone domain) {
            super(project, domain);
            this.openstackClient = getOpenstackClient();
        }

        @Override
        public Void call() throws CloudException, IOException {
            Zone zone = getZone();

            List<Recordset> requested = readFromDatabase(true);
            List<Recordset> current;
            try {
                current = readFromOpenstack(zone);
            } catch (RestClientException e) {
                throw new CloudException("Error reading zone", e);
            }

            Changes changes = computeChanges(current, requested);

            try {
                OpenstackDnsClient client = openstackClient.getDns();
                for (Recordset r : changes.remove) {
                    client.deleteRecordset(zone.id, r.id);
                }
                for (Recordset r : changes.create) {
                    client.createRecordset(zone.id, r);
                }
            } catch (RestClientException e) {
                throw new CloudException("Error applying zone changes", e);
            }

            return null;
        }

        private List<Recordset> readFromOpenstack(Zone zone) throws RestClientException {
            OpenstackDnsClient client = openstackClient.getDns();

            List<Recordset> recordsets = client.listRecordsets(zone.id, true);
            return recordsets;
        }

        private Zone getZone() throws CloudException {
            String zoneName = this.zone.getName();
            try {
                OpenstackDnsClient client = openstackClient.getDns();
                List<Zone> zones = client.listZones();
                Zone zone = null;
                for (Zone z : zones) {
                    if (zoneName.equals(z.name)) {
                        zone = z;
                        break;
                    }
                }

                if (zone == null) {
                    zone = new Zone();
                    zone.name = zoneName;

                    zone = client.createZone(zone);
                }

                return zone;
            } catch (RestClientException e) {
                throw new CloudException("Error mapping zone: " + zoneName, e);
            }
        }
    }

}
