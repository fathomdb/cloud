package io.fathom.cloud.commands;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

@Singleton
public class SshCommandFactory implements CommandFactory {

    private static final Logger log = LoggerFactory.getLogger(SshCommandFactory.class);

    @Inject
    Cmdlets cmdlets;

    public Command parseCommand(Cmdlet cmdlet, List<String> tokens) throws CmdLineException {
        String line = Joiner.on(" ").join(tokens);

        try {
            List<String> args = tokens.subList(1, tokens.size());
            cmdlet.parseArguments(line, args);
        } catch (CmdLineException e) {
            throw e;
        } catch (Exception e) {
            // TODO: Print nice message??
            throw new IllegalArgumentException("Error parsing tokens", e);
        }

        return new SshCommand(cmdlet);
    }

    public Cmdlet buildCommand(List<String> tokens) throws CmdLineException {
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Command is required");
        }

        String command = tokens.get(0);

        Cmdlet cmdlet = cmdlets.getCommand(command);
        if (cmdlet == null) {
            cmdlet = cmdlets.getCommand("help");
        }

        return cmdlet;
    }

    @Override
    public Command createCommand(String line) {
        log.info("Running SSH command: " + line);

        List<String> tokens = Lists.newArrayList();
        StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(line));

        tokenizer.resetSyntax();

        tokenizer.wordChars(33, 128);
        tokenizer.wordChars(128 + 32, 255);

        tokenizer.whitespaceChars(0, ' ');
        tokenizer.quoteChar('"');
        tokenizer.quoteChar('\'');

        boolean done = false;
        while (!done) {
            int nextToken;
            try {
                nextToken = tokenizer.nextToken();
            } catch (IOException e) {
                log.warn("Error parsing line: {}", line);
                throw new IllegalArgumentException("Unable to parse line: " + line, e);
            }
            switch (nextToken) {
            case StreamTokenizer.TT_NUMBER:
                throw new IllegalStateException();
            case StreamTokenizer.TT_WORD:
                tokens.add(tokenizer.sval);
                break;

            case StreamTokenizer.TT_EOF:
                done = true;
                break;

            case StreamTokenizer.TT_EOL:
                break;

            case '\"':
            case '\'':
                tokens.add(tokenizer.sval);
                break;

            default:
                log.warn("Error parsing line: {}", line);
                throw new IllegalArgumentException("Unable to parse line: " + line);
            }
        }

        Cmdlet cmdlet = null;
        try {
            cmdlet = buildCommand(tokens);
            Command command = parseCommand(cmdlet, tokens);
            return command;
        } catch (CmdLineException e) {
            Command command = new PrintErrorCommand(e.getMessage(), cmdlet);
            return command;
        } catch (Exception e) {
            log.warn("Error building command for line: " + line, e);
            throw new IllegalArgumentException("Error building command for line: " + line, e);
        }
    }

}
