package io.fathom.cloud.identity.commands;

import io.fathom.cloud.commands.Cmdlet;
import io.fathom.cloud.identity.LoginService;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.keyczar.KeyczarFactory;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;

import java.io.File;

import javax.inject.Inject;

import org.keyczar.KeyczarFileReader;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.io.IoUtils;

public class PasswordChangeCmdlet extends Cmdlet {
    public PasswordChangeCmdlet() {
        super("id-password-change");
    }

    private static final Logger log = LoggerFactory.getLogger(PasswordChangeCmdlet.class);

    @Inject
    IdentityService identityService;

    @Inject
    LoginService loginService;

    @Inject
    KeyczarFactory keyczarFactory;

    @Option(name = "-o", usage = "path for private key store", required = false)
    public File path;

    @Option(name = "-u", usage = "username", required = true)
    public String username;

    @Option(name = "-p", usage = "password", required = true)
    public String password;

    @Override
    public void run() throws Exception {
        if (path == null) {
            path = IoUtils.resolve("~/passwordrecovery");
        }

        log.info("Loading password-recovery key from {}", path);

        KeyczarFileReader store = new KeyczarFileReader(path.getAbsolutePath());

        DomainData domain = identityService.getDefaultDomain();
        loginService.changePassword(domain, username, password, store);
    }
}
