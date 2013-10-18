package io.fathom.cloud.compute.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class Limits {
    @XmlElement(name = "absolute")
    public AbsoluteLimits absoluteLimits;

    @XmlElement(name = "rate")
    public List<RateLimit> rates;

}
