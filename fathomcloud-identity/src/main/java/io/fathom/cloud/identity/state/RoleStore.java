package io.fathom.cloud.identity.state;

import io.fathom.cloud.WellKnownRoles;
import io.fathom.cloud.protobuf.IdentityModel.RoleData;

import java.util.List;

import com.google.common.collect.Lists;

public class RoleStore {

    public List<RoleData> list() {
        List<RoleData> roles = Lists.newArrayList();

        roles.add(find(WellKnownRoles.ROLE_ID_ADMIN));
        roles.add(find(WellKnownRoles.ROLE_ID_MEMBER));

        return roles;
    }

    public RoleData find(long roleId) {
        if (roleId == WellKnownRoles.ROLE_ID_ADMIN) {
            RoleData.Builder b = RoleData.newBuilder();
            b.setId(WellKnownRoles.ROLE_ID_ADMIN);
            b.setName("Admin");
            return b.build();
        }

        if (roleId == WellKnownRoles.ROLE_ID_MEMBER) {
            RoleData.Builder b = RoleData.newBuilder();
            b.setId(WellKnownRoles.ROLE_ID_MEMBER);
            b.setName("Member");
            return b.build();
        }
        return null;
    }

}
