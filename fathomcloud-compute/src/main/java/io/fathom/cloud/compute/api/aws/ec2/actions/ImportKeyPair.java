package io.fathom.cloud.compute.api.aws.ec2.actions;

import io.fathom.cloud.compute.api.aws.ec2.model.ImportKeyPairResponse;

import com.fathomdb.utils.Hex;

@AwsAction("ImportKeyPair")
public class ImportKeyPair extends AwsActionHandler {
    @Override
    public Object go() {
        String publicKeyMaterial = get("PublicKeyMaterial");
        String keyName = get("KeyName");

        // byte[] publicKey = Base64.decode(publicKeyMaterial);
        //
        // String s = new String(publicKey);
        // s = s.replace('\t', ' ');
        // s = s.replace('\r', ' ');
        // s = s.replace('\n', ' ');
        // s = s.trim();
        //
        // if (s.startsWith("ssh-rsa ")) {
        // s = s.substring(8);
        // }
        // s = s.trim();
        //
        // byte[] data = Base64.decode(s);
        // Md5Hash hash = Md5Hash.Hasher.INSTANCE.hash(data);
        // byte[] hashBytes = hash.toByteArray();

        // Apparently this is the MD5 of the public key in DER encoding
        // "openssl pkey -in ~/.ssh/ec2/primary.pem -pubout -outform DER | openssl md5 -c"
        byte[] hashBytes = new byte[16];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hashBytes.length; i++) {
            if (i != 0) {
                sb.append(":");
            }
            sb.append(Hex.toHex(hashBytes[i]));
        }

        ImportKeyPairResponse response = new ImportKeyPairResponse();
        response.requestId = getRequestId();
        response.keyName = keyName;

        response.keyFingerprint = sb.toString().toLowerCase();

        return response;
    }
}
