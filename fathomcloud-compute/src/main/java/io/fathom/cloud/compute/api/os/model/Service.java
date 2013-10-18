package io.fathom.cloud.compute.api.os.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;

public class Service {

    public String binary;
    public String host;
    public String zone;

    public String state;
    public String status;

    @XmlElement(name = "updated_at")
    public Date updatedAt;

    @XmlElement(name = "disabled_reason")
    public String disabledReason;

}
