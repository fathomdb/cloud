package io.fathom.cloud.secrets.api.os.model;

import java.util.Date;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Secret {
    public String id;

    public String status;
    public String name;
    public String algorithm;
    public String mode;
    public Integer bit_length;
    public Map<String, String> content_types;

    public Date updated;
    public Date expiration;

    public String secret_href;

    public String subject;
}
