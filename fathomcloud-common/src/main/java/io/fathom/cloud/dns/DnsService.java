package io.fathom.cloud.dns;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.state.DuplicateValueException;

import java.util.List;

public interface DnsService {
    public interface Zone {
        String getName();

        long getProjectId();

        long getId();
    }

    public class DnsZoneSpec {
        public String name;
        public String backend;
    }

    public class DnsRecordSpec {
        public String address;
    }

    public class DnsRecordsetSpec {
        public String fqdn;
        public String type;
        public List<DnsRecordSpec> records;
    }

    public interface Recordset {
        String getFqdn();

        String getType();

        Zone getZone();

        List<? extends Record> getRecords();
    }

    public interface Record {
        String getTarget();
    }

    public static final String TYPE_SOA = "SOA";
    public static final String TYPE_A = "A";
    public static final String TYPE_AAAA = "AAAA";
    public static final String TYPE_NS = "NS";

    // Record buildAddress(String fqdn, InetAddress ip);

    Zone createZone(Project project, DnsZoneSpec zone) throws CloudException, DuplicateValueException;

    void setDnsRecordsets(String systemKey, Project project, List<DnsRecordsetSpec> dnsRecordsets)
            throws CloudException;

    Zone findZoneByName(Project project, String domainName) throws CloudException;

    Zone findDomain(Project project, long id) throws CloudException;

    List<? extends Recordset> listRecordsets(Project project, Zone domain) throws CloudException;

    Recordset createRecordset(Project project, Zone domain, String name, String type, List<String> ips)
            throws CloudException;

    List<? extends Zone> listZones(Project project) throws CloudException;

    Recordset findRecordset(Project project, Zone domain, long recordsetId) throws CloudException;

}
