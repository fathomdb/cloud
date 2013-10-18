package io.fathom.cloud.identity.api.os.model.v3;

import java.util.List;

public class Domain {
    public String id;
    public String name;

    public Boolean enabled;
    public String description;

    public List<Link> links;
}