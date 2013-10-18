package io.fathom.cloud.identity.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.identity.state.AuthRepository;
import io.fathom.cloud.protobuf.IdentityModel.CredentialData;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.state.NamedItemCollection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.persist.Transactional;

@Singleton
public class Sweeper {

    private static final Logger log = LoggerFactory.getLogger(Sweeper.class);

    @Inject
    AuthRepository repository;

    public void sweep() throws CloudException {
        for (DomainData domain : repository.getDomains().list()) {
            sweep(domain);
        }
    }

    @Transactional
    public void sweep(DomainData domain) throws CloudException {
        NamedItemCollection<CredentialData> credentials = repository.getUsernames(domain);

        for (CredentialData credential : credentials.list()) {
            long userId = credential.getUserId();

            UserData user = repository.getUsers().find(userId);
            if (user == null) {
                log.warn("Removing credential that references deleted user: {}", credential);

                credentials.delete(credential.getKey());
            }
        }
    }

}
