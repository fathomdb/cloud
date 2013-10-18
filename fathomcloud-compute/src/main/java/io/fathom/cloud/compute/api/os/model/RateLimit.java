package io.fathom.cloud.compute.api.os.model;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class RateLimit {

    public static class SubLimit {
        public Date nextAvailable;

        public int remaining;
        public String unit;
        public int value;
        public String verb;
    }

    @XmlElement(name = "limit")
    public List<SubLimit> limits;

    public String regex;
    public String uri;
}
