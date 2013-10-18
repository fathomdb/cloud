package io.fathom.cloud.network.api.os.models;

import java.util.List;

public class Port {
    public String id;

    public boolean admin_state_up;
    public String device_id;
    public String device_owner;
    public List<FixedIp> fixes_ips;
    public String mac_address;
    public String name;
    public String network_id;
    public List<String> security_groups;
    public String status;
    public String tenant_id;

    public static class FixedIp {
        public String ip_address;
        public String subnet_id;
    }
}
