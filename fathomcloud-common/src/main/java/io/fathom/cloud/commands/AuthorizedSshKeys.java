package io.fathom.cloud.commands;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.crypto.OpenSshUtils;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class AuthorizedSshKeys {
    private static final Logger log = LoggerFactory.getLogger(AuthorizedSshKeys.class);

    final List<PublicKey> publicKeys;

    public AuthorizedSshKeys(File file) throws IOException {
        publicKeys = readKeys(file);
    }

    private List<PublicKey> readKeys(File file) throws IOException {
        return readKeys(Files.readLines(file, Charsets.UTF_8));
    }

    private List<PublicKey> readKeys(Iterable<String> lines) throws IOException {
        List<PublicKey> publicKeys = Lists.newArrayList();
        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            try {
                PublicKey publicKey = OpenSshUtils.readSshPublicKey(line);
                publicKeys.add(publicKey);
            } catch (IllegalArgumentException e) {
                log.warn("Error reading ssh key line: " + line, e);
            }
        }
        return publicKeys;
    }
}
