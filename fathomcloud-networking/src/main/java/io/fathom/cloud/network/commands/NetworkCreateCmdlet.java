package io.fathom.cloud.network.commands;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.commands.Cmdlet;
import io.fathom.cloud.network.NetworkService;
import io.fathom.cloud.protobuf.NetworkingModel.NetworkData;
import io.fathom.cloud.protobuf.ProtobufYamlWriter;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.services.AuthService;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkCreateCmdlet extends Cmdlet {
    private static final Logger log = LoggerFactory.getLogger(NetworkCreateCmdlet.class);

    public NetworkCreateCmdlet() {
        super("network-create");
    }

    @Option(name = "-u", usage = "username", required = true)
    public String username;

    @Option(name = "-p", usage = "password", required = true)
    public String password;

    @Option(name = "-project", usage = "project", required = true)
    public String project;

    @Option(name = "-n", usage = "name", required = true)
    public String name;

    public boolean adminStateUp = true;
    public boolean shared = true;
    public boolean routerExternal = true;

    @Inject
    AuthService authService;

    @Inject
    NetworkService networkService;

    @Override
    public void run() throws CloudException, IOException {
        Auth unscoped = authService.authenticate(null, username, password);
        if (unscoped == null) {
            throw new IllegalArgumentException("Cannot authenticate");
        }
        List<Long> projectIds = authService.resolveProjectName(unscoped, project);

        if (projectIds.size() == 0) {
            throw new IllegalArgumentException("Cannot find project");
        }
        if (projectIds.size() != 1) {
            throw new IllegalArgumentException("The project name is ambiguous");
        }
        Long projectId = projectIds.get(0);

        Auth auth = authService.authenticate(projectId, username, password);
        if (auth == null) {
            throw new IllegalArgumentException("Cannot authenticate to project");
        }

        NetworkData.Builder b = NetworkData.newBuilder();
        if (name != null) {
            b.setName(name);
        }

        b.setAdminStateUp(adminStateUp);
        b.setShared(shared);
        b.setRouterExternal(routerExternal);

        NetworkData created = networkService.createNetwork(auth, b);

        try (Writer writer = new OutputStreamWriter(stdout)) {
            ProtobufYamlWriter.serialize(created, writer);
        }
    }

    // @Override
    // public void setSession(ServerSession session) {
    // SshAgent sshAgent =
    // session.getFactoryManager().getAgentFactory().createClient(session);
    //
    // sshAgent.sign(, );
    // }
}
