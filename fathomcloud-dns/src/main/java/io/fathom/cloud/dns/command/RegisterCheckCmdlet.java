package io.fathom.cloud.dns.command;

import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.openstack.client.identity.CertificateAuthTokenProvider;
import io.fathom.cloud.openstack.client.identity.ChallengeResponses;
import io.fathom.cloud.openstack.client.identity.OpenstackIdentityClient;
import io.fathom.cloud.openstack.client.identity.model.V2ProjectList;
import io.fathom.cloud.ssh.SshContext;
import io.fathom.cloud.ssh.jsch.SshContextImpl;

import java.net.URI;
import java.security.KeyPair;

import javax.inject.Inject;
import javax.security.auth.x500.X500Principal;

import org.kohsuke.args4j.Option;

import com.fathomdb.crypto.CertificateAndKey;

public class RegisterCheckCmdlet extends TypedCmdlet {
    @Inject
    SshContext sshContext;

    @Option(name = "-s", usage = "server", required = false, metaVar = "URL")
    public String server = "https://api-cloud.fathomdb.com/openstack/identity/";

    public RegisterCheckCmdlet() {
        super("dns-register-check");
    }

    @Override
    protected V2ProjectList run0() throws Exception {
        KeyPair keypair = ((SshContextImpl) sshContext).getKeypair();

        URI uri = URI.create(server);
        OpenstackIdentityClient identityClient = OpenstackIdentityClient.build(uri);

        X500Principal subject = new X500Principal("CN=" + "test");
        CertificateAndKey certificateAndKey = ChallengeResponses.createSelfSigned(subject, keypair);

        String project = null;

        CertificateAuthTokenProvider tokenProvider = CertificateAuthTokenProvider.build(identityClient, project,
                certificateAndKey);

        identityClient = identityClient.withTokenProvider(tokenProvider);

        // V2AuthRequest authRequest = new V2AuthRequest();
        // authRequest.auth = new V2AuthRequest.V2AuthCredentials();
        //
        // V2AuthResponse authResponse = client.doLogin(authRequest,
        // certificateAndKey);
        //
        // V2AuthRequest.ChallengeResponse challengeResponse =
        // ChallengeResponses.respondToAuthChallenge(
        // certificateAndKey, authResponse.challenge);
        // authRequest.auth.challengeResponse = challengeResponse;
        //
        // authResponse = client.doLogin(authRequest, certificateAndKey);

        V2ProjectList projects = identityClient.listProjects();

        return projects;
    }
}
