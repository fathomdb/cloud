package io.fathom.cloud.compute.api.os.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Aggregate {
    public String id;
    public String name;

    public String availability_zone;
    public List<String> hosts;
    public Map<String, String> metadata;

    public Boolean deleted;

    public Date created_at;
    public Date updated_at;
    public Date deleted_at;
}
