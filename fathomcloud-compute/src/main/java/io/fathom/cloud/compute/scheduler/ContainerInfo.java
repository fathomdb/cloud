package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.DebugFormatter;
import io.fathom.cloud.services.ImageKey;

public class ContainerInfo {

    public String key;

    public ImageKey imageId;

    public LxcConfigBuilder lxcConfig;

    // public List<InjectFile> injectFiles;
    //
    // public static class InjectFile {
    // public String path;
    //
    // public int mode;
    //
    // public byte[] contents;
    //
    // @Override
    // public String toString() {
    // return DebugFormatter.format(this);
    // }
    // }

    @Override
    public String toString() {
        return DebugFormatter.format(this);
    }

}
