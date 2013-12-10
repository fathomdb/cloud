package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.ssh.SshConfig;

import java.io.File;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class ShellCommand {
    final List<String> args = Lists.newArrayList();

    ShellCommand(String[] args) {
        for (String arg : args) {
            this.args.add(arg);
        }
    }

    public static ShellCommand create(String... args) {
        return new ShellCommand(args);
    }

    public void arg(File file) {
        argQuoted(file.getAbsolutePath());
    }

    public ShellCommand literal(String literal) {
        args.add(literal);
        return this;
    }

    public void argQuoted(String s) {
        StringBuilder escaped = new StringBuilder();
        escaped.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if ('a' <= c && c <= 'z') {

            } else if ('A' <= c && c <= 'Z') {

            } else if ('0' <= c && c <= '9') {

            } else {
                switch (c) {
                case '-':
                case '_':
                case '/':
                case ':':
                case '.':
                    break;

                default:
                    throw new IllegalArgumentException("Don't know how to escape character: " + c);
                }
            }

            escaped.append(c);
        }
        escaped.append('"');

        args.add(escaped.toString());
    }

    public void useSudo() {
        args.add(0, "sudo");
    }

    public SshCommand withSsh(SshConfig sshConfig) {
        String sshCommand = Joiner.on(' ').join(args);
        return new SshCommand(sshConfig, sshCommand);
    }

}
