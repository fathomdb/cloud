package io.fathom.cloud.loadbalancer;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.openstack.client.loadbalance.model.LbaasMapping;
import io.fathom.cloud.openstack.client.loadbalance.model.LbaasServer;
import io.fathom.cloud.server.model.Project;

import java.util.List;

public interface LoadBalanceService {

    void setMappings(String systemKey, Project project, List<LbaasMapping> mappings) throws CloudException;

    void setServers(String systemKey, Project project, List<LbaasServer> servers) throws CloudException;

}
