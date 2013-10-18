package io.fathom.cloud.compute.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Hypervisors {
    public List<Hypervisor> hypervisors;
}
