package io.fathom.cloud.commands;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.discovery.DiscoveredSubTypes;
import com.fathomdb.discovery.Discovery;
import com.google.common.collect.Lists;
import com.google.inject.Injector;

@Singleton
public class Cmdlets {

    private static final Logger log = LoggerFactory.getLogger(Cmdlets.class);

    @Inject
    Discovery discovery;

    @Inject
    Injector injector;

    List<Cmdlet> cmdlets;

    synchronized List<Cmdlet> getCommands() {
        if (cmdlets == null) {
            List<Cmdlet> cmdlets = Lists.newArrayList();
            DiscoveredSubTypes<Cmdlet> cmdletTypes = discovery.getSubTypesOf(Cmdlet.class);

            for (Cmdlet cmdlet : cmdletTypes.getInstances()) {
                cmdlets.add(cmdlet);

                log.info("Added cmdlet: {}", cmdlet.getClass().getSimpleName());
            }

            this.cmdlets = cmdlets;
        }
        return cmdlets;
    }

    public Cmdlet getCommand(String command) {
        List<Cmdlet> matching = Lists.newArrayList();

        for (Cmdlet cmdlet : getCommands()) {
            if (cmdlet.accepts(command)) {
                matching.add(cmdlet);
            }
        }

        if (matching.size() == 0) {
            return null;
        }

        if (matching.size() != 1) {
            throw new IllegalArgumentException("Ambiguous command");
        }

        Cmdlet cmdlet = injector.getInstance(matching.get(0).getClass());
        return cmdlet;
    }

}
