package io.fathom.cloud.compute.api.os.model;

import javax.xml.bind.annotation.XmlElement;

public class WrappedQuotaSet {

    @XmlElement(name = "quota_set")
    public QuotaSet quotaSet;

}
