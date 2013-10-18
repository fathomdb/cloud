package io.fathom.cloud.compute.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.state.ComputeRepository;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupRuleData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.state.NumberedItemCollection;
import io.fathom.cloud.state.StateStoreException;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.persist.Transactional;

@Transactional
@Singleton
public class SecurityGroups {

    @Inject
    ComputeRepository repository;

    @Inject
    AsyncTasks asyncTasks;

    @Inject
    ComputeServices computeServices;

    protected NumberedItemCollection<SecurityGroupData> getStore(Project project) throws CloudException {
        return repository.getSecurityGroups(project.getId());
    }

    public SecurityGroupData find(Project project, long id) throws CloudException {
        return getStore(project).find(id);
    }

    // public SecurityGroupData update(Project project,
    // SecurityGroupData.Builder securityGroup) throws CloudException {
    // SecurityGroupData updated = getStore(project).update(securityGroup);
    //
    // applySecurityGroup(project, updated);
    //
    // return updated;
    // }

    private void applySecurityGroup(Auth auth, Project project, SecurityGroupData securityGroup) throws CloudException {
        long securityGroupId = securityGroup.getId();

        Set<Long> hostIds = Sets.newHashSet();

        List<InstanceData> instances = computeServices.listInstances(auth, project);
        for (InstanceData instance : instances) {
            // TODO: Only if instance alive??

            boolean dead = false;
            switch (instance.getInstanceState()) {
            case STOPPED:
            case TERMINATED:
                dead = true;
                break;

            default:
                // Apply (even if stopping, because it's still running)
                break;
            }

            if (dead) {
                continue;
            }

            if (instance.getSecurityGroupIdList().contains(securityGroupId)) {
                long hostId = instance.getHostId();

                hostIds.add(hostId);
            }
        }

        for (long hostId : hostIds) {
            asyncTasks.updateSecurityGroupDefinition(project, securityGroup, hostId);
        }
    }

    public List<SecurityGroupData> list(Project project) throws CloudException {
        return getStore(project).list();
    }

    public SecurityGroupData delete(Project project, long id) throws StateStoreException, CloudException {
        return getStore(project).delete(id);
    }

    public List<SecurityGroupData> getSecurityGroups(Project project, InstanceData instance) throws CloudException {
        if (instance.getProjectId() != project.getId()) {
            throw new IllegalArgumentException();
        }

        List<SecurityGroupData> securityGroups = Lists.newArrayList();

        {
            NumberedItemCollection<SecurityGroupData> securityGroupsStore = getStore(project);

            for (long securityGroupId : instance.getSecurityGroupIdList()) {
                SecurityGroupData securityGroup = securityGroupsStore.find(securityGroupId);
                if (securityGroup == null) {
                    throw new IllegalArgumentException();
                }
                securityGroups.add(securityGroup);
            }
        }

        return securityGroups;
    }

    public SecurityGroupRuleData addRule(Auth auth, Project project, long securityGroupId,
            SecurityGroupRuleData.Builder sgb) throws CloudException {
        NumberedItemCollection<SecurityGroupData> store = getStore(project);
        SecurityGroupData securityGroupData = store.find(securityGroupId);
        if (securityGroupData == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        SecurityGroupData.Builder b = SecurityGroupData.newBuilder(securityGroupData);
        SecurityGroupRuleData rule = sgb.build();
        b.addRules(rule);

        SecurityGroupData updated = store.update(b);
        applySecurityGroup(auth, project, updated);

        return rule;
    }

    public SecurityGroupData create(Project project, SecurityGroupData.Builder b) throws CloudException {
        b.setProjectId(project.getId());
        return getStore(project).create(b);
    }

    public SecurityGroupData deleteRule(Auth auth, Project project, long ruleId) throws CloudException {
        // This sort of sucks, because we don't have an index on ruleId
        NumberedItemCollection<SecurityGroupData> store = getStore(project);
        SecurityGroupData securityGroupData = null;
        int ruleIndex = -1;

        for (SecurityGroupData g : store.list()) {
            for (int i = 0; i < g.getRulesCount(); i++) {
                SecurityGroupRuleData r = g.getRules(i);
                if (r.getId() == ruleId) {
                    securityGroupData = g;
                    ruleIndex = i;
                    break;
                }
            }
        }

        if (securityGroupData == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        SecurityGroupData.Builder b = SecurityGroupData.newBuilder(securityGroupData);
        b.removeRules(ruleIndex);

        SecurityGroupData updated = store.update(b);
        applySecurityGroup(auth, project, updated);

        return updated;
    }

    public SecurityGroupData find(Project project, String name) throws CloudException {
        if (Strings.isNullOrEmpty(name)) {
            return null;
        }
        for (SecurityGroupData sg : getStore(project).list()) {
            if (name.equals(sg.getName())) {
                return sg;
            }
        }
        return null;
    }

    public InstanceData addRemoveSecurityGroup(Project project, long instanceId, SecurityGroupData sg, boolean remove)
            throws CloudException {
        NumberedItemCollection<InstanceData> store = repository.getInstances(project.getId());
        InstanceData instance = store.find(instanceId);
        if (instance == null) {
            throw new IllegalArgumentException();
        }

        InstanceData.Builder b = InstanceData.newBuilder(instance);
        Set<Long> securityGroups = Sets.newHashSet(b.getSecurityGroupIdList());
        if (remove) {
            securityGroups.remove(sg.getId());
        } else {
            securityGroups.add(sg.getId());
        }

        b.clearSecurityGroupId();
        b.addAllSecurityGroupId(securityGroups);

        InstanceData updated = store.update(b);
        asyncTasks.updateInstanceSecurityGroups(project, updated);
        return updated;
    }
}
