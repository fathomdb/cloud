package io.fathom.cloud.identity.commands;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;
import io.fathom.cloud.protobuf.IdentityModel.RoleData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;

public class DomainRoleGrantCmdlet extends TypedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(DomainRoleGrantCmdlet.class);

    public DomainRoleGrantCmdlet() {
        super("id-domainrole-grant");
    }

    @Option(name = "-touser", usage = "user name", required = true)
    public String grantee;

    @Option(name = "-r", usage = "role", required = true)
    public String roleName;

    @Inject
    IdentityService identityService;

    @Override
    public Message run0() throws CloudException {
        doDomainGrant();
        return null;
    }

    private UserData getGrantee(DomainData domain) throws CloudException {
        UserData user = identityService.findUserByName(domain.getId(), grantee);
        if (user == null) {
            throw new IllegalArgumentException("Cannot find user: " + grantee);
        }

        return user;
    }

    private RoleData getRole() throws CloudException {
        RoleData role = null;
        for (RoleData r : identityService.listRoles()) {
            if (roleName.equalsIgnoreCase(r.getName())) {
                role = r;
            }
        }
        if (role == null) {
            throw new IllegalArgumentException("Cannot find role: " + roleName);
        }
        return role;
    }

    private void doDomainGrant() throws CloudException {
        // Domain grant
        DomainData domain = identityService.getDefaultDomain();

        RoleData role = getRole();

        UserData user = getGrantee(domain);

        log.info("Doing domain grant: {} {}", user.getName(), role.getName());
        identityService.grantDomainRoleToUser(domain.getId(), user.getId(), role.getId());
    }
}
