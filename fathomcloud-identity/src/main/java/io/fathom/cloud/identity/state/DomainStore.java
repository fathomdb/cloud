package io.fathom.cloud.identity.state;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;

import java.util.List;

import com.google.common.collect.Lists;

public class DomainStore {
    public static final long DEFAULT_DOMAIN_ID = 1;

    public DomainData find(long domainId) {
        if (domainId != DEFAULT_DOMAIN_ID) {
            return null;
        }
        DomainData.Builder b = DomainData.newBuilder();
        b.setId(DEFAULT_DOMAIN_ID);
        b.setDescription("Default domain");
        b.setEnabled(true);
        b.setName("default");
        return b.build();
    }

    protected DomainData findDefaultDomain() throws CloudException {
        return find(DEFAULT_DOMAIN_ID);
    }

    public DomainData getDefaultDomain() throws CloudException {
        DomainData domain = findDefaultDomain();
        if (domain == null) {
            throw new IllegalStateException();
        }
        return domain;
    }

    public List<DomainData> list() throws CloudException {
        List<DomainData> ret = Lists.newArrayList();
        ret.add(getDefaultDomain());
        return ret;
    }

}
