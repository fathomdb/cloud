package io.fathom.cloud.storage;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.server.model.Project;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.io.ByteSource;

public interface FileService {
    void putFile(Project project, String bucketName, String name, FileBlob fileData, String contentType,
            Map<String, String> userAttributes, FilePutOption... options) throws CloudException, IOException;

    void deleteFile(Project project, String bucketName, String name) throws CloudException, IOException;

    FileInfo getFileInfo(Project project, String bucketName, String name) throws CloudException, IOException;

    public interface FileInfo {
        long getLength();

        String getPath();

        boolean isDirectory();
    }

    List<? extends FileInfo> listFiles(Project project, String bucketName, String prefix, String delimiter)
            throws CloudException;

    void append(Project project, String bucketName, String name, Long offset, FileBlob fileData) throws CloudException,
            IOException;

    ByteSource getData(Project project, String bucketName, String name, Long from, Long to) throws CloudException,
            IOException;

    void ensureBucket(Project project, String bucketName) throws CloudException;

}
