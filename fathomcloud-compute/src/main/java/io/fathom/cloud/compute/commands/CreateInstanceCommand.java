package io.fathom.cloud.compute.commands;

//
//import java.util.List;
//
//import javax.inject.Inject;
//import javax.inject.Provider;
//
//import org.kohsuke.args4j.Option;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import io.fathom.cloud.CloudException;
//import io.fathom.cloud.actions.StartInstancesAction;
//import io.fathom.cloud.commands.TypedCmdlet;
//import io.fathom.cloud.compute.api.os.resources.OpenstackIds;
//import io.fathom.cloud.compute.api.os.resources.SecurityGroupDictionary;
//import io.fathom.cloud.compute.services.ComputeServices;
//import io.fathom.cloud.compute.services.KeyPairs;
//import io.fathom.cloud.compute.services.SecurityGroups;
//import io.fathom.cloud.protobuf.CloudModel.InstanceInfo;
//import io.fathom.cloud.protobuf.CloudModel.KeyPairData;
//import io.fathom.cloud.protobuf.CloudModel.ReservationInfo;
//import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
//import io.fathom.cloud.server.auth.Auth;
//import io.fathom.cloud.server.model.Project;
//import io.fathom.cloud.services.AuthService;
//import io.fathom.cloud.services.ImageService;
//
//public class CreateInstanceCommand extends TypedCmdlet {
//    private static final Logger log = LoggerFactory.getLogger(CreateInstanceCommand.class);
//
//    @Option(name = "-ip", usage = "ip", required = true)
//    public String ip;
//
//    @Option(name = "-image", usage = "image", required = true)
//    public String imageRef;
//
//    @Option(name = "-name", usage = "name", required = true)
//    public String name;
//
//    @Option(name = "-key", usage = "key", required = true)
//    public String keyName;
//
//    @Option(name = "-sg", usage = "security group", required = true)
//    public List<String> securityGroupNames;
//
//    @Option(name = "-u", usage = "username", required = true)
//    public String username;
//
//    @Option(name = "-p", usage = "password", required = true)
//    public String password;
//
//    @Inject
//    ComputeServices computeServices;
//
//    @Inject
//    SecurityGroups securityGroups;
//
//    public CreateInstanceCommand() {
//        super("create-instance");
//    }
//
//    @Inject
//    Provider<StartInstancesAction> startInstancesActionProvider;
//
//    @Inject
//    ImageService imageService;
//
//    @Inject
//    KeyPairs keypairs;
//
//    @Inject
//    AuthService authService;
//
//    Auth authenticate() throws CloudException {
//        Auth auth = authService.authenticate(null, username, password);
//        if (auth == null) {
//            throw new IllegalStateException("Authentication failed");
//        }
//        return auth;
//    }
//
//    @Override
//    protected InstanceInfo run0() throws Exception {
//        authenticate();
//
//        StartInstancesAction action = startInstancesActionProvider.get();
//
//        Auth auth = authenticate();
//        Project project = auth.getProject();
//
//        action.project = project;
//        action.auth = auth;
//
//        action.minCount = 1;
//        action.maxCount = 1;
//
//        ImageService.Image image;
//        {
//            ReservationInfo.Builder reservation = ReservationInfo.newBuilder();
//
//            long imageId = OpenstackIds.toImageId(imageRef);
//            image = imageService.findImage(project, imageId);
//            if (image == null) {
//                throw new IllegalArgumentException("Image not found");
//            }
//
//            // TODO: Copy image?
//            reservation.setImageId(image.getId());
//
//            action.reservationTemplate = reservation.build();
//        }
//
//        {
//            InstanceInfo.Builder instance = InstanceInfo.newBuilder();
//            instance.setName(name);
//
//            if (keyName != null) {
//                KeyPairData keypair = keypairs.findKeyPair(project, keyName);
//                if (keypair == null) {
//                    throw new IllegalArgumentException();
//                }
//                instance.setKeyPair(keypair);
//            }
//
//            instance.setImageId(image.getId());
//
//            if (securityGroupNames != null && !securityGroupNames.isEmpty()) {
//                SecurityGroupDictionary dictionary = new SecurityGroupDictionary(securityGroups.list(project));
//
//                for (String securityGroupName : securityGroupNames) {
//                    SecurityGroupData data = dictionary.getByName(securityGroupName);
//                    if (data == null) {
//                        throw new IllegalArgumentException("Security group not found: " + securityGroupName);
//                    }
//
//                    instance.addSecurityGroupId(data.getId());
//                }
//            }
//
//            action.instanceTemplate = instance.build();
//        }
//
//        StartInstancesAction.Result result = action.go();
//
//        if (result.instances.size() != 1) {
//            throw new IllegalStateException();
//        }
//
//        return result.instances.get(0);
//    }
// }
