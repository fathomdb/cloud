package io.fathom.cloud.install;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.compute.actions.StartInstancesAction;
import io.fathom.cloud.compute.services.DerivedMetadata;
import io.fathom.cloud.compute.services.SecurityGroups;
import io.fathom.cloud.compute.services.SshKeyPairs;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.KeyPairData;
import io.fathom.cloud.protobuf.CloudModel.MetadataData;
import io.fathom.cloud.protobuf.CloudModel.MetadataEntryData;
import io.fathom.cloud.protobuf.CloudModel.ReservationData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupRuleData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.services.AuthService;
import io.fathom.cloud.services.ImageImports;
import io.fathom.cloud.services.ImageService;
import io.fathom.cloud.services.ImageService.Image;
import io.fathom.cloud.ssh.SshContext;

import java.io.IOException;
import java.security.PublicKey;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class InstanceBootstrapCmdlet extends TypedCmdlet {

    private static final Logger log = LoggerFactory.getLogger(InstanceBootstrapCmdlet.class);

    @Option(name = "-u", usage = "username", required = true)
    public String username;

    @Option(name = "-p", usage = "password", required = true)
    public String password;

    @Option(name = "-url", usage = "image url", required = true)
    public String imageUrl;

    // @Option(name = "-ip", usage = "ip", required = true)
    // public String ip;

    @Option(name = "-name", usage = "name", required = true)
    public String name;

    @Option(name = "-host", usage = "host")
    public String host;

    public InstanceBootstrapCmdlet() {
        super("compute-instance-bootstrap");
    }

    @Inject
    Provider<StartInstancesAction> startInstancesActionProvider;

    @Inject
    AuthService authService;

    @Inject
    SshKeyPairs keypairs;

    @Inject
    SecurityGroups securityGroups;

    @Inject
    ImageService imageService;

    @Inject
    ImageImports imageImports;

    @Inject
    SshContext sshContext;

    private Auth auth;
    private Project project;

    @Override
    protected InstanceData run0() throws Exception {
        authenticate();

        KeyPairData keyPair = buildKeypair();

        List<Long> securityGroupIds = buildSecurityGroups();

        Image image = buildImage();

        return startInstance(image, securityGroupIds, keyPair);
    }

    private void authenticate() throws CloudException {
        auth = authService.authenticate(null, username, password);
        if (auth == null) {
            throw new IllegalStateException();
        }
        log.info("Authenticated as: {}", username);

        String projectName = "__system__";
        List<Long> projectIds = authService.resolveProjectName(auth, projectName);
        Long projectId;
        if (projectIds.isEmpty()) {
            log.info("Creating project: {}", projectName);
            projectId = authService.createProject(auth, projectName);
        } else {
            if (projectIds.size() != 1) {
                throw new IllegalStateException("Found multiple projects with name: " + projectName);
            }
            projectId = projectIds.get(0);
            log.info("Found project: {}", projectName);
        }

        auth = authService.authenticate(projectId, username, password);
        if (auth == null) {
            throw new IllegalStateException("Error authenticating to project");
        }
        log.info("Authenticated to project");

        project = auth.getProject();
    }

    private KeyPairData buildKeypair() throws CloudException, IOException {
        PublicKey sshPublicKey = sshContext.getPublicKey();

        String keypairName = "__system__";
        KeyPairData keyPair = keypairs.findKeyPair(project, keypairName);
        if (keyPair == null) {
            log.info("Creating keypair: {}", keypairName);
            keypairs.create(project, keypairName, sshPublicKey);
        } else {
            log.info("Found keypair: {}", keypairName);
        }
        return keyPair;
    }

    private List<Long> buildSecurityGroups() throws CloudException {
        List<Long> securityGroupIds = Lists.newArrayList();

        String securityGroupName = "__system__";
        SecurityGroupData securityGroup = securityGroups.find(project, securityGroupName);
        if (securityGroup == null) {
            log.info("Creating security group: {}", securityGroupName);

            SecurityGroupData.Builder b = SecurityGroupData.newBuilder();
            b.setName(securityGroupName);
            b.setProjectId(project.getId());
            securityGroup = securityGroups.create(project, b);
        } else {
            log.info("Found security group: {}", securityGroupName);
        }

        if (securityGroup.getRulesCount() == 0) {
            SecurityGroupRuleData.Builder sgb = SecurityGroupRuleData.newBuilder();
            sgb.setFromSecurityGroup(securityGroup.getId());
            securityGroups.addRule(auth, project, securityGroup.getId(), sgb);
        }

        securityGroupIds.add(securityGroup.getId());
        return securityGroupIds;
    }

    private Image buildImage() throws Exception, CloudException {
        ImageImports.Metadata metadata = imageImports.getImageMetadata(imageUrl);

        List<Image> images = imageService.listImages(project);
        Image image = null;
        for (Image i : images) {
            String imageChecksum = i.getChecksum();
            if (imageChecksum.equalsIgnoreCase(metadata.getChecksum())) {
                image = i;
                break;
            }
        }

        if (image == null) {
            log.info("Importing image from: {}", imageUrl);
            image = imageImports.importImage(project.getId(), imageUrl);
        } else {
            log.info("Found image: {}", image.getName());
        }
        return image;
    }

    private InstanceData startInstance(Image image, List<Long> securityGroupIds, KeyPairData keyPair)
            throws CloudException {
        StartInstancesAction action = startInstancesActionProvider.get();

        action.project = project;
        action.auth = auth;

        action.minCount = 1;
        action.maxCount = 1;

        {
            ReservationData.Builder reservation = ReservationData.newBuilder();

            reservation.setImageId(image.getId());

            action.reservationTemplate = reservation.build();
        }

        {
            InstanceData.Builder instance = InstanceData.newBuilder();
            instance.setName(name);

            if (keyPair != null) {
                instance.setKeyPair(keyPair);
            }

            {
                MetadataData.Builder metadataBuilder = instance.getMetadataBuilder();

                if (host != null) {
                    MetadataEntryData.Builder entryBuilder = metadataBuilder.addEntryBuilder();
                    entryBuilder.setKey(DerivedMetadata.KEY_DNS_HOST);
                    entryBuilder.setValue(host);
                }
            }

            instance.setImageId(image.getId());

            for (Long securityGroupId : securityGroupIds) {
                instance.addSecurityGroupId(securityGroupId);
            }

            action.instanceTemplate = instance.build();
        }

        // action.ip = this.ip;

        log.info("Starting instance");

        StartInstancesAction.Result result = action.go();

        if (result.instances.size() != 1) {
            throw new IllegalStateException();
        }

        return result.instances.get(0);
    }

}
