package io.fathom.cloud.compute.api.os.model;

public class Record {
    public String ip;
    public String domain;
    public String name;

    // It seems to be dns_type in, and type out (?)
    public String type;
    public String dns_type;

}
