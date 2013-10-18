package io.fathom.cloud.compute.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class Certificates {
    @XmlElement(name = "certificate")
    public List<Certificate> certificates;
}
