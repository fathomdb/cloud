package io.fathom.cloud.compute.api.os.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class WrappedKeypair {
    public Keypair keypair;
}
