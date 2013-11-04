package io.fathom.cloud;

public class ServiceType {

    public static final ServiceType COMPUTE = new ServiceType("compute");
    public static final ServiceType IMAGE = new ServiceType("image");
    public static final ServiceType EC2 = new ServiceType("ec2");
    public static final ServiceType IDENTITY = new ServiceType("identity");
    public static final ServiceType VOLUME = new ServiceType("volume");
    public static final ServiceType NETWORK = new ServiceType("network");
    public static final ServiceType OBJECT_STORE = new ServiceType("object-store");
    public static final ServiceType ORCHESTRATION = new ServiceType("orchestration");
    public static final ServiceType DBAAS = new ServiceType("database");
    public static final ServiceType SECRETS = new ServiceType("keystore");
    public static final ServiceType DNS = new ServiceType("dns");

    final String type;
    final String name;

    public ServiceType(String type) {
        this(type, type);
    }

    public ServiceType(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getUrlSuffix() {
        return type;
    }

    public String getName() {
        return name;
    }

}
