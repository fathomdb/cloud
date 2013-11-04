package io.fathom.cloud;

import static org.junit.Assert.assertEquals;
import io.fathom.cloud.identity.services.KeyPairs;
import io.fathom.cloud.openstack.client.OpenstackClient;
import io.fathom.cloud.openstack.client.identity.AuthTokenProvider;
import io.fathom.cloud.openstack.client.identity.OpenstackIdentityClient;
import io.fathom.cloud.openstack.client.identity.TokenProvider;
import io.fathom.cloud.openstack.client.identity.model.V2ProjectList;
import io.fathom.cloud.ssh.SshConfig;
import io.fathom.cloud.ssh.SshContext;
import io.fathom.cloud.ssh.jsch.SshContextImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyPair;
import java.security.Security;
import java.util.Properties;

import org.apache.curator.test.TestingServer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fathomdb.Configuration;
import com.fathomdb.config.ConfigurationImpl;
import com.fathomdb.crypto.OpenSshUtils;
import com.google.common.base.Charsets;

public class TestCloudServer {
    static TestingServer zkTestServer;

    static Path workdir;

    static CloudServer server;

    static SshContext sshContext;

    @BeforeClass
    public static void startServer() throws Exception {
        zkTestServer = new TestingServer(2181);

        workdir = Files.createTempDirectory("unittest");

        Properties properties = new Properties();
        properties.setProperty("zookeeper.servers", zkTestServer.getConnectString());
        properties.setProperty("metadata.host", "127.0.0.1");

        KeyPair sshKeyPair = KeyPairs.generateKeyPair();

        Path privateKey = workdir.resolve("id_rsa");
        Path publicKey = workdir.resolve("id_rsa.pub");
        Files.write(privateKey, KeyPairs.serializePem(sshKeyPair.getPrivate()).getBytes(Charsets.UTF_8));
        Files.write(publicKey, OpenSshUtils.serialize(sshKeyPair.getPublic()).getBytes(Charsets.UTF_8));

        Path authorizedKeysPath = workdir.resolve("authorized_keys");
        properties.setProperty("sshd.authorized_keys", "authorized_keys");
        String authorizedKeys = OpenSshUtils.serialize(sshKeyPair.getPublic()) + " unittest";
        Files.write(authorizedKeysPath, authorizedKeys.getBytes(Charsets.UTF_8));

        sshContext = new SshContextImpl("root", privateKey.toFile());

        Configuration configuration = ConfigurationImpl.from(workdir.toFile(), properties);

        server = CloudServer.build(configuration);
        server.start();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        server.stop();
        java.nio.file.Files.walkFileTree(workdir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

        });
    }

    @Test
    public void testLogin() throws Exception {
        URI identityUri = URI.create("http://127.0.0.1:8080/openstack/identity/");
        OpenstackIdentityClient identityClient = OpenstackIdentityClient.build(identityUri);
        String username = "admin";
        String password = "secret";
        String project = "__system__";

        runSshCommand("id-passwordrecovery-create", "-o", workdir.resolve("passwordrecovery").toString());

        runSshCommand("id-user-create", "-u", username, "-p", password);

        runSshCommand("id-domainrole-grant", "-touser", username, "-r", "admin");

        runSshCommand("id-project-create", "-u", username, "-p", password, "-proj", project);
        runSshCommand("id-role-grant", "-u", username, "-p", password, "-touser", username, "-proj", project, "-r",
                "admin");

        TokenProvider tokenProvider = AuthTokenProvider.build(identityClient, project, username, password);
        OpenstackClient client = OpenstackClient.build(tokenProvider);

        V2ProjectList projects = client.getIdentity().listProjects();

        assertEquals(1, projects.tenants.size());
        assertEquals(project, projects.tenants.get(0).name);
        assertEquals(true, projects.tenants.get(0).enabled);
    }

    private void runSshCommand(String... args) throws Exception {
        SshConfig sshConfig = sshContext.buildConfig(new InetSocketAddress("127.0.0.1", 2222));

        StringBuilder cmd = new StringBuilder();
        for (String arg : args) {
            if (cmd.length() != 0) {
                cmd.append(' ');
            }

            // TODO: Check for spaces and quote??
            cmd.append(arg);
        }

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = sshConfig.execute(cmd.toString(), stdout, stderr);

        if (exitCode != 0) {
            System.out.println("Command: " + cmd);
            System.out.println("STDOUT: " + new String(stdout.toByteArray(), Charsets.UTF_8));
            System.out.println("STDERR: " + new String(stderr.toByteArray(), Charsets.UTF_8));
            throw new IllegalStateException("Non-zero exit code returned: " + exitCode);
        }
    }
}
