package io.fathom.cloud.commands;

import java.io.File;

import com.fathomdb.io.IoUtils;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class PublicKeyGetCmdlet extends Cmdlet {
    public PublicKeyGetCmdlet() {
        super("pubkey-get");
    }

    @Override
    public void run() throws Exception {
        File path = IoUtils.resolve("~/.ssh/id_rsa.pub");

        if (!path.exists()) {
            printerr("SSH key not found");
            throw new IllegalStateException();
        }

        String publicKey = Files.toString(path, Charsets.UTF_8);
        println(publicKey);
    }
}
