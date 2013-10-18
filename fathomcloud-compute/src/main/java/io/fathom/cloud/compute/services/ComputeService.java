package io.fathom.cloud.compute.services;

import io.fathom.cloud.lifecycle.LifecycleListener;

public interface ComputeService extends LifecycleListener {

    void purgeDeletedInstances();
}
