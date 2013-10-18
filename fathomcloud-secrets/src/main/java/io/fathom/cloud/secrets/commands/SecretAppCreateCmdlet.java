package io.fathom.cloud.secrets.commands;

import io.fathom.cloud.commands.AuthenticatedCmdlet;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.Attachments;
import io.fathom.cloud.services.Attachments.ClientApp;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;

public class SecretAppCreateCmdlet extends AuthenticatedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(SecretAppCreateCmdlet.class);

    public SecretAppCreateCmdlet() {
        super("secret-app-create");
    }

    @Option(name = "-name", usage = "app name", required = true)
    public String appName;

    @Option(name = "-secret", usage = "app secret", required = true)
    public String appSecret;

    @Inject
    Attachments attachments;

    @Override
    protected Message run0() throws Exception {
        Auth auth = getAuth();
        Project project = auth.getProject();

        ClientApp clientApp = attachments.createClientApp(auth, project, appName, appSecret);

        // TODO: Return something useful
        return null;
    }
}
