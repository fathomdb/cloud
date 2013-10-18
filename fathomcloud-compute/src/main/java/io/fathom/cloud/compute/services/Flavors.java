package io.fathom.cloud.compute.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.state.ComputeRepository;
import io.fathom.cloud.lifecycle.LifecycleListener;
import io.fathom.cloud.protobuf.CloudModel.FlavorData;
import io.fathom.cloud.state.DuplicateValueException;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.persist.Transactional;

@Transactional
@Singleton
public class Flavors implements LifecycleListener {
    private static final Logger log = LoggerFactory.getLogger(Flavors.class);

    @Inject
    ComputeRepository computeRepository;

    void initialize() throws DuplicateValueException, CloudException {
        List<FlavorData> flavors = computeRepository.getFlavors().list();

        // TODO: This isn't atomic
        if (flavors.isEmpty()) {
            createFlavor("m1.tiny", 512, 1, 1, 0);
            createFlavor("m1.small", 2048, 1, 20, 0);
            createFlavor("m1.medium", 4096, 2, 40, 0);
            createFlavor("m1.large", 8192, 4, 80, 0);
            createFlavor("m1.xlarge", 16384, 8, 160, 0);
        }
    }

    private void createFlavor(String name, int ram, int vcpus, int disk, int ephemeral) throws DuplicateValueException,
            CloudException {
        FlavorData.Builder b = FlavorData.newBuilder();
        b.setName(name);
        b.setRam(ram);
        b.setDisk(disk);
        // b.setSwap(0);
        b.setVcpus(vcpus);
        b.setEphemeral(ephemeral);

        computeRepository.getFlavors().create(b);
    }

    @Override
    public void start() throws Exception {
        initialize();
    }

    public List<FlavorData> list() throws CloudException {
        List<FlavorData> flavors = computeRepository.getFlavors().list();
        return flavors;
    }

    public FlavorData find(long id) throws CloudException {
        return computeRepository.getFlavors().find(id);
    }
}
