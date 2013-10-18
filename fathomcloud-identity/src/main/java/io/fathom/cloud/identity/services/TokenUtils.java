package io.fathom.cloud.identity.services;

import io.fathom.cloud.identity.Users;
import io.fathom.cloud.identity.state.AuthRepository;
import io.fathom.cloud.protobuf.IdentityModel.ProjectRoles;
import io.fathom.cloud.protobuf.IdentityModel.RoleData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.Lists;

public class TokenUtils {

    @Inject
    AuthRepository authRepository;

    public List<RoleData> getProjectRoles(UserData user, long projectId) {
        ProjectRoles found = Users.findProjectRoles(user, projectId);
        if (found == null) {
            return Collections.emptyList();
        }

        List<RoleData> ret = Lists.newArrayList();
        for (long roleId : found.getRoleList()) {
            RoleData role = authRepository.getRoles().find(roleId);
            if (role == null) {
                continue;
            }
            ret.add(role);
        }
        return ret;
    }
}
