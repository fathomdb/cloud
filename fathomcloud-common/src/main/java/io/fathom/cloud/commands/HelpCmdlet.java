package io.fathom.cloud.commands;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Prints help
 */
public class HelpCmdlet extends Cmdlet {
    private static final Logger log = LoggerFactory.getLogger(HelpCmdlet.class);

    public HelpCmdlet() {
        super("help");
    }

    @Inject
    Cmdlets cmdlets;

    @Override
    protected void run() throws Exception {
        List<Cmdlet> commands = cmdlets.getCommands();

        List<String> lines = Lists.newArrayList();
        for (Cmdlet cmdlet : commands) {
            lines.add(cmdlet.getCommand());
        }

        Collections.sort(lines);

        for (String line : lines) {
            println(line);
        }
    }
}
