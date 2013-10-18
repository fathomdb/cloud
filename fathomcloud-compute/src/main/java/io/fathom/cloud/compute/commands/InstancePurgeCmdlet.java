package io.fathom.cloud.compute.commands;

import io.fathom.cloud.commands.Cmdlet;
import io.fathom.cloud.compute.services.Instances;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.TimeSpan;

public class InstancePurgeCmdlet extends Cmdlet {
    private static final Logger log = LoggerFactory.getLogger(InstancePurgeCmdlet.class);

    @Inject
    Instances instances;

    @Option(name = "-age", usage = "age", required = false)
    public TimeSpan age = TimeSpan.ONE_DAY;

    public InstancePurgeCmdlet() {
        super("compute-instance-purge");
    }

    @Override
    protected void run() throws Exception {
        instances.purgeDeletedInstances(age);
    }

}
