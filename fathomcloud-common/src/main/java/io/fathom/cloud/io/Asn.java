package io.fathom.cloud.io;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

public class Asn {
    static class AsnInputStream implements Closeable {
        final InputStream is;

        static final int MAX_BUFFER_SIZE = 32768;

        public AsnInputStream(InputStream is) {
            super();
            this.is = is;
        }

        @Override
        public void close() throws IOException {
            is.close();
        }

        int readUint8() throws IOException {
            int v = is.read();
            if (v == -1) {
                throw new IOException("EOF");
            }
            return v;
        }

        long readPacked() throws IOException {
            int b = readUint8();
            if ((b & 0x80) == 0) {
                return b & 0x7f;
            } else {
                int len = b & 0x7f;
                long value = 0;
                for (int i = 0; i < len; i++) {
                    b = readUint8();

                    value <<= 8;
                    value |= b;
                }
                return value;
            }
        }

        // long readUint32() throws IOException {
        // long value = readUint8();
        // value <<= 8;
        // value |= readUint8();
        // value <<= 8;
        // value |= readUint8();
        // value <<= 8;
        // value |= readUint8();
        // return value;
        // }

        public byte[] readByteArray() throws IOException {
            long length = readPacked();
            if (length > MAX_BUFFER_SIZE) {
                throw new IOException("Byte array too large");
            }
            byte[] buffer = new byte[(int) length];
            ByteStreams.readFully(is, buffer, 0, (int) length);
            return buffer;
        }

        // public String readString() throws IOException {
        // return Utf8.toString(readByteArray());
        // }

        public BigInteger readInteger() throws IOException {
            int tag = readTag();
            if (tag != 0x02) {
                throw new IOException("Expected INTEGER tag");
            }
            byte[] data = readByteArray();
            if (data.length == 0) {
                return BigInteger.ZERO;
            }

            return new BigInteger(data);
        }

        public int readTag() throws IOException {
            int tag = readUint8();
            return tag;
        }

        public int readSequenceTag() throws IOException {
            int tag = readTag();
            if (tag != 48) {
                throw new IOException("Expected sequence tag");
            }
            return tag;
        }
    }

    public static PublicKey readRsaPublicKey(byte[] data) throws IOException, InvalidKeySpecException {
        Asn.AsnInputStream is = new Asn.AsnInputStream(new ByteArrayInputStream(data));
        try {
            is.readSequenceTag();
            long length = is.readPacked();

            final BigInteger modulus = is.readInteger();
            final BigInteger publicExponent = is.readInteger();

            final RSAPublicKeySpec rsaPubSpec = new RSAPublicKeySpec(modulus, publicExponent);

            try {
                KeyFactory rsaKeyFact = KeyFactory.getInstance("RSA");
                return rsaKeyFact.generatePublic(rsaPubSpec);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Error loading RSA provider", e);
            }
        } finally {
            Closeables.closeQuietly(is);
        }
    }
}
