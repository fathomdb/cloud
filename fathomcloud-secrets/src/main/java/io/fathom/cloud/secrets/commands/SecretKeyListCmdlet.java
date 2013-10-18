package io.fathom.cloud.secrets.commands;

import io.fathom.cloud.commands.AuthenticatedCmdlet;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.SecretService;
import io.fathom.cloud.services.SecretService.Secret;
import io.fathom.cloud.services.SecretService.SecretInfo;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;

public class SecretKeyListCmdlet extends AuthenticatedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(SecretKeyListCmdlet.class);

    public SecretKeyListCmdlet() {
        super("secret-key-list");
    }

    @Inject
    SecretService secretService;

    @Override
    protected Message run0() throws Exception {
        Auth auth = getAuth();
        Project project = auth.getProject();

        List<Secret> secrets = secretService.list(auth, project);

        for (Secret secret : secrets) {
            SecretInfo secretInfo = secret.getSecretInfo();
            println(secret.getId() + "\t" + secretInfo.name);
        }

        return null;
    }
}
