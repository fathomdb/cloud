package io.fathom.cloud.dbaas.api.os.model;

import java.util.List;

public class DatabaseUser {
    public String name;
    public String password;
    public String host;

    public List<Database> databases;
}
