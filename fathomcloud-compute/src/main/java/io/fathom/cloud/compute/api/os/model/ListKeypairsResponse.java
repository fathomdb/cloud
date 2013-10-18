package io.fathom.cloud.compute.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ListKeypairsResponse {
    @XmlElementWrapper(name = "keypair")
    public List<Keypair> keypairs;
}
