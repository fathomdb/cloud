package io.fathom.cloud.secrets.api.os.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SecretList {
    public List<Secret> secrets;
}
