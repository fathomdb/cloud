package io.fathom.cloud.dbaas.api.os.model;

import java.util.List;

public class DbInstance {
    public String name;
    public String flavorRef;

    public DatabaseVolume volume;

    public List<Database> databases;

    public List<DatabaseUser> users;

    public String service_type;

    public RestorePoint restorePoint;

    public static class RestorePoint {
        public String backupRef;
    }

}
