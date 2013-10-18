package io.fathom.cloud.compute.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.state.ComputeRepository;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.MetadataData;
import io.fathom.cloud.protobuf.CloudModel.MetadataEntryData;
import io.fathom.cloud.server.model.Project;

import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

@Transactional
@Singleton
public class MetadataServices {

    private static final Logger log = LoggerFactory.getLogger(MetadataServices.class);

    @Inject
    ComputeRepository computeRepository;

    @Inject
    ComputeDerivedMetadata magicMetadata;

    InstanceData findInstance(Project project, long instanceId) throws CloudException {
        return computeRepository.getInstances(project.getId()).find(instanceId);
    }

    public InstanceData replaceMetadata(Project project, long instanceId, Map<String, String> model, boolean replaceAll)
            throws CloudException {
        InstanceData instance = findInstance(project, instanceId);
        if (instance == null) {
            throw new IllegalArgumentException();
        }

        InstanceData.Builder b = InstanceData.newBuilder(instance);
        if (replaceAll) {
            b.clearMetadata();

            MetadataData.Builder mb = b.getMetadataBuilder();

            for (Entry<String, String> entry : model.entrySet()) {
                MetadataEntryData.Builder eb = mb.addEntryBuilder();
                eb.setKey(entry.getKey());
                eb.setValue(entry.getValue());
            }
        } else {
            MetadataData.Builder mb = b.getMetadataBuilder();

            for (Entry<String, String> entry : model.entrySet()) {
                if (!setMetadataKey(mb, entry.getKey(), entry.getValue())) {
                    throw new IllegalArgumentException();
                }
            }
        }

        InstanceData updated = computeRepository.getInstances(project.getId()).update(b);
        magicMetadata.instanceUpdated(project, updated);
        return updated;
    }

    public static Map<String, String> toMap(MetadataData metadata) {
        Map<String, String> model = Maps.newHashMap();
        if (metadata != null) {
            for (MetadataEntryData entry : metadata.getEntryList()) {
                String key = entry.getKey();
                String value = entry.getValue();

                model.put(key, value);
            }
        }

        return model;
    }

    public InstanceData replaceMetadataKey(Project project, long instanceId, String key, String value)
            throws CloudException {
        InstanceData instance = findInstance(project, instanceId);
        if (instance == null) {
            throw new IllegalArgumentException();
        }
        InstanceData.Builder b = InstanceData.newBuilder(instance);

        MetadataData.Builder mb = b.getMetadataBuilder();

        if (!setMetadataKey(mb, key, value)) {
            throw new IllegalArgumentException();
        }

        InstanceData updated = computeRepository.getInstances(project.getId()).update(b);
        magicMetadata.instanceUpdated(project, updated);
        return updated;
    }

    private boolean setMetadataKey(io.fathom.cloud.protobuf.CloudModel.MetadataData.Builder mb, String key,
            String value) {
        boolean found = false;
        for (int i = 0; i < mb.getEntryCount(); i++) {
            MetadataEntryData.Builder eb = mb.getEntryBuilder(i);
            if (key.equals(eb.getKey())) {
                found = true;
                if (value == null) {
                    mb.removeEntry(i);
                } else {
                    eb.setValue(value);
                }
                break;
            }
        }

        if (!found) {
            if (value == null) {
                return false;
            } else {
                MetadataEntryData.Builder eb = mb.addEntryBuilder();
                eb.setKey(key);
                eb.setValue(value);
            }
        }

        return true;
    }

}
