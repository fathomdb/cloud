package io.fathom.cloud.compute.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.state.ComputeRepository;
import io.fathom.cloud.protobuf.CloudModel.KeyPairData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.state.DuplicateValueException;
import io.fathom.cloud.state.NamedItemCollection;

import java.security.PublicKey;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.keyczar.DefaultKeyType;
import org.keyczar.KeyMetadata;
import org.keyczar.KeyczarKey;
import org.keyczar.KeyczarUtils;
import org.keyczar.enums.KeyPurpose;

import com.fathomdb.crypto.OpenSshUtils;
import com.google.inject.persist.Transactional;

@Singleton
@Transactional
public class SshKeyPairs {
    @Inject
    ComputeRepository repository;

    public KeyPairData findKeyPair(Project project, String id) throws CloudException {
        return getKeyPairsStore(project).find(id);
    }

    NamedItemCollection<KeyPairData> getKeyPairsStore(Project project) throws CloudException {
        return repository.getKeypairs(project.getId());
    }

    public List<KeyPairData> list(Project project) throws CloudException {
        return getKeyPairsStore(project).list();
    }

    public KeyPairData create(Project project, KeyPairData.Builder keyPairData) throws CloudException,
            DuplicateValueException {
        return getKeyPairsStore(project).create(keyPairData);
    }

    public boolean delete(Project project, String id) throws CloudException {
        NamedItemCollection<KeyPairData> store = getKeyPairsStore(project);
        KeyPairData keypair = store.find(id);
        if (keypair == null) {
            return false;
        }
        store.delete(id);
        return true;
    }

    public KeyczarKey generateKeypair() {
        try {
            KeyczarKey keypair = KeyczarUtils.createKey(new KeyMetadata("RSA Key", KeyPurpose.DECRYPT_AND_ENCRYPT,
                    DefaultKeyType.RSA_PRIV));
            return keypair;
        } catch (Exception e) {
            throw new IllegalStateException("Error generating keypair", e);
        }
    }

    public KeyPairData create(Project project, String name, PublicKey publicKey) throws CloudException {
        KeyPairData.Builder keyPairData = KeyPairData.newBuilder();
        keyPairData.setKey(name);

        String publicKeyString = OpenSshUtils.serialize(publicKey);
        keyPairData.setPublicKey(publicKeyString);

        String publicKeyFingerprint = OpenSshUtils.getSignatureString(publicKey);
        if (publicKeyFingerprint != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < publicKeyFingerprint.length(); i += 2) {
                if (i != 0) {
                    sb.append(":");
                }
                sb.append(publicKeyFingerprint.substring(i, i + 2));
            }
            keyPairData.setPublicKeyFingerprint(sb.toString());
        }

        try {
            KeyPairData created = create(project, keyPairData);
            return created;
        } catch (DuplicateValueException e) {
            throw new WebApplicationException(Status.CONFLICT);
        }
    }
}
