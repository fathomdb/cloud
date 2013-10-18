package io.fathom.cloud.dns.services;

import io.fathom.cloud.CloudException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class WellKnownTlds {
    private static final Logger log = LoggerFactory.getLogger(WellKnownTlds.class);

    @Inject
    DnsServiceImpl dns;

    public void create() {
        String[] tlds = new String[] { "com", "net", "org", "biz", "us", "me", "io", "info", "name" };

        for (String tld : tlds) {
            try {
                dns.ensureTld(tld);
            } catch (CloudException e) {
                log.warn("Error creating TLD: " + tld, e);
            }
        }
    }

}
