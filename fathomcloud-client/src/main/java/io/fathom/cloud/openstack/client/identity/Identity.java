package io.fathom.cloud.openstack.client.identity;

import io.fathom.cloud.openstack.client.RestClientException;
import io.fathom.cloud.openstack.client.identity.model.V2Project;
import io.fathom.cloud.openstack.client.identity.model.V2ProjectList;
import io.fathom.cloud.openstack.client.identity.model.V3Project;

import java.util.UUID;

/**
 * High level functions
 * 
 */
public class Identity {

    final OpenstackIdentityClient identityClient;

    public Identity(OpenstackIdentityClient identityClient) {
        this.identityClient = identityClient;
    }

    public String ensureProjectWithPrefix(String prefix) throws RestClientException {
        V2Project found = null;
        V2ProjectList projects = identityClient.listProjects();
        for (V2Project project : projects.tenants) {
            if (project.name == null) {
                continue;
            }

            if (project.name.startsWith(prefix)) {
                found = project;
                break;
            }
        }

        if (found == null) {
            V3Project project = new V3Project();
            project.name = prefix + UUID.randomUUID().toString();
            V3Project created = identityClient.createProject(project);

            return created.name;
        } else {
            return found.name;
        }
    }
}
