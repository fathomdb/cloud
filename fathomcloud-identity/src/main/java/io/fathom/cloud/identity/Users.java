package io.fathom.cloud.identity;

import io.fathom.cloud.protobuf.IdentityModel.ProjectRoles;
import io.fathom.cloud.protobuf.IdentityModel.UserData;

public class Users {
    public static ProjectRoles findProjectRoles(UserData user, long projectId) {
        ProjectRoles projectRoles = null;
        for (ProjectRoles i : user.getProjectRolesList()) {
            if (i.getProject() == projectId) {
                projectRoles = i;
                break;
            }
        }
        return projectRoles;
    }
}
