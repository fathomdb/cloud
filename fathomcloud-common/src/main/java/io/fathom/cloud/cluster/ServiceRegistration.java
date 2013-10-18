package io.fathom.cloud.cluster;

import io.fathom.cloud.CloudException;

import com.google.inject.ImplementedBy;

@ImplementedBy(ServiceRegistrationImpl.class)
public interface ServiceRegistration {
    public void register() throws CloudException;
}
