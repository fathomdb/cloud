package io.fathom.cloud.ssh.mina;

//
//import java.security.KeyPair;
//import java.security.KeyPairGenerator;
//import java.security.NoSuchAlgorithmException;
//import java.security.PrivateKey;
//import java.security.PublicKey;
//
//public class SshKey {
//	private static final String ALGORITHM = "RSA";
//	public static final int DEFAULT_KEYSIZE = 2048;
//
//	private final KeyPair keyPair;
//
//	public SshKey(KeyPair keyPair) {
//		this.keyPair = keyPair;
//	}
//
//	static KeyPair generateKeyPair(String algorithm, int keysize) {
//		KeyPairGenerator generator;
//		try {
//			generator = KeyPairGenerator.getInstance(algorithm);
//		} catch (NoSuchAlgorithmException e) {
//			throw new IllegalStateException("Error loading crypto provider", e);
//		}
//		generator.initialize(keysize);
//		KeyPair keyPair = generator.generateKeyPair();
//		return keyPair;
//	}
//
//	public static SshKey generate() {
//		KeyPair keyPair = generateKeyPair(ALGORITHM, DEFAULT_KEYSIZE);
//		return new SshKey(keyPair);
//	}
//
//	public PublicKey getPublicKey() {
//		return keyPair.getPublic();
//	}
//
//	public PrivateKey getPrivateKey() {
//		return keyPair.getPrivate();
//	}
// }
