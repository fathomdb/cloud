package io.fathom.cloud.secrets.commands;

import io.fathom.cloud.commands.AuthenticatedCmdlet;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.SecretService;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;

public class SecretKeyDeleteCmdlet extends AuthenticatedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(SecretKeyDeleteCmdlet.class);

    public SecretKeyDeleteCmdlet() {
        super("secret-key-delete");
    }

    @Option(name = "-id", usage = "key id", required = true)
    public Long id;

    @Inject
    SecretService secretService;

    @Override
    protected Message run0() throws Exception {
        Auth auth = getAuth();
        Project project = auth.getProject();

        secretService.deleteKey(auth, project, id);
        return null;
    }
}
