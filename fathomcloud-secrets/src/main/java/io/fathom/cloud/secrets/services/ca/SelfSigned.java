package io.fathom.cloud.secrets.services.ca;

import io.fathom.cloud.CloudException;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import com.fathomdb.crypto.bouncycastle.SimpleCertificateAuthority;
import com.google.common.collect.Lists;

public class SelfSigned {

    X500Principal buildPrincipal(String subject) throws CloudException {
        String x500Name = "CN=" + subject;
        // if (contactInfo != null) {
        // x500Name += buildCountry(contactInfo);
        // }
        X500Principal principal = new X500Principal(x500Name);
        return principal;
    }

    // protected String buildCountry(String country) {
    // // TODO: Normalize country, fix..
    // if (country.equalsIgnoreCase("USA")) {
    // country = "US";
    // }
    //
    // return ", C=" + country;
    // }

    public Csr buildCsr(KeyPair keyPair, String subject) throws CloudException {
        String domainName = subject;
        if (domainName == null) {
            throw new CloudException("Subject must be specified");
        }

        X500Principal principal = buildPrincipal(subject);

        Csr csr = Csr.buildCsr(keyPair, principal);

        return csr;
    }

    public List<X509Certificate> selfSign(Csr csr, KeyPair keyPair) throws CloudException {
        X509Certificate certificate = SimpleCertificateAuthority.selfSign(csr.getEncoded(), keyPair);

        List<X509Certificate> chain = Lists.newArrayList();
        chain.add(certificate);

        return chain;
    }
}
