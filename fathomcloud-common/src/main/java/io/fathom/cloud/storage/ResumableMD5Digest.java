package io.fathom.cloud.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.protobuf.ByteString;

/**
 * Allows us to grab the MD5 state
 */
public class ResumableMD5Digest {
    private static final MessageDigest MASTER;
    private static final Field FIELD_DIGESTSPI;
    private static final Field FIELD_STATE;
    private static final Field FIELD_BYTESPROCESSED;

    private static final Field FIELD_BUFFER;
    private static final Field FIELD_BUFFEROFFSET;

    final MessageDigest md;
    final int[] state;

    private Object sunmd5;

    public ResumableMD5Digest() {
        try {
            this.md = (MessageDigest) MASTER.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Unable to clone MD5", e);
        }

        try {
            this.sunmd5 = FIELD_DIGESTSPI.get(this.md);
            int[] state = (int[]) FIELD_STATE.get(sunmd5);

            this.state = state;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to access state in MD5 object", e);
        }
    }

    static {
        try {
            MASTER = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to get MD5", e);
        }

        try {
            Class<? extends MessageDigest> clazz = MASTER.getClass();
            FIELD_DIGESTSPI = getField(clazz, "digestSpi");

            Class<?> md5Class = sun.security.provider.MD5.class;
            FIELD_STATE = getField(md5Class, "state");

            Class<?> digestBaseClass = md5Class.getSuperclass();
            FIELD_BYTESPROCESSED = getField(digestBaseClass, "bytesProcessed");

            FIELD_BUFFER = getField(digestBaseClass, "buffer");
            FIELD_BUFFEROFFSET = getField(digestBaseClass, "bufOfs");
        } catch (Exception e) {
            throw new IllegalStateException("Error finding internal fields of MD5 implementation", e);
        }
    }

    private static Field getField(Class<?> clazz, String name) throws NoSuchFieldException, SecurityException {
        // for (Field field : clazz.getDeclaredField(name)()) {
        // if (field.getName().equals(name)) {
        // field.setAccessible(true);
        // return field;
        // }
        // }
        // throw new IllegalStateException("Unable to find field: " + name);
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static ResumableMD5Digest get() {
        // TODO: Pool objects?
        ResumableMD5Digest md5 = new ResumableMD5Digest();
        md5.reset();
        return md5;
    }

    private void reset() {
        this.md.reset();
    }

    public void update(byte[] data) {
        this.md.update(data);
    }

    public void update(byte[] data, int offset, int length) {
        this.md.update(data, offset, length);
    }

    public byte[] digest() {
        // TODO: Return to pool now?
        return this.md.digest();
    }

    public ByteString getState() {
        try {
            int bufferOffset = FIELD_BUFFEROFFSET.getInt(sunmd5);

            ByteBuffer buf = ByteBuffer.allocate(16 + bufferOffset);
            for (int i = 0; i < state.length; i++) {
                buf.putInt(state[i]);
            }

            if (bufferOffset != 0) {
                byte[] buffer = (byte[]) FIELD_BUFFER.get(sunmd5);
                buf.put(buffer, 0, bufferOffset);
            }

            buf.flip();
            return ByteString.copyFrom(buf);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Error getting MD5 state", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Error getting MD5 state", e);
        }
    }

    public void setState(ByteString state, long length) {
        this.md.reset();
        ByteBuffer buf = state.asReadOnlyByteBuffer();
        for (int i = 0; i < this.state.length; i++) {
            this.state[i] = buf.getInt();
        }
        int bufferOffset = buf.remaining();

        try {
            if (bufferOffset != 0) {
                byte[] buffer = (byte[]) FIELD_BUFFER.get(sunmd5);
                buf.get(buffer, 0, bufferOffset);
            }

            FIELD_BUFFEROFFSET.setInt(sunmd5, bufferOffset);
            FIELD_BYTESPROCESSED.setLong(sunmd5, length);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Error setting bytesProcessed", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Error setting bytesProcessed", e);
        }
    }

    public void update(InputStream is) throws IOException {
        byte[] buf = new byte[4096];
        while (true) {
            int n = is.read(buf);
            if (n == -1) {
                break;
            }
            update(buf, 0, n);
        }
    }

    public void update(InputSupplier<? extends InputStream> iss) throws IOException {
        try (InputStream is = iss.getInput()) {
            update(is);
        }
    }

    public void update(File file) throws IOException {
        update(Files.newInputStreamSupplier(file));
    }

    public void update(ByteSource src) throws IOException {
        try (InputStream is = src.openStream()) {
            update(is);
        }
    }
}
