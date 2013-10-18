package io.fathom.auto.zookeeper.model;

import com.google.common.base.Strings;

public class ClusterState {
    public String createdBy;

    public boolean isCreated() {
        return !Strings.isNullOrEmpty(createdBy);
    }

}
