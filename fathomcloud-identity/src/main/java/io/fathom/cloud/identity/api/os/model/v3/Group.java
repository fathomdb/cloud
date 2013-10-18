package io.fathom.cloud.identity.api.os.model.v3;

import java.util.List;

public class Group {
    public String id;
    public String name;

    public String description;
    public String domain_id;

    public List<Link> links;
}