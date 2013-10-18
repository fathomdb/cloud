package io.fathom.cloud.lbaas.services;

import io.fathom.cloud.server.model.Project;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.protobuf.GeneratedMessage;

public abstract class Mapper<Data extends GeneratedMessage, Model> {
    /**
     * Constructs a model with the DB only fields cleared. We can then compare
     * this against values not from the database.
     */
    public abstract Data toComparable(Data d);

    public abstract Data toData(Project project, String systemKey, Model model);

    public abstract Model toModel(Data data);

    public List<Data> toData(Project project, String systemKey, List<Model> models) {
        List<Data> ret = Lists.newArrayList();

        for (Model model : models) {
            Data data = toData(project, systemKey, model);
            ret.add(data);
        }

        return ret;
    }

}
