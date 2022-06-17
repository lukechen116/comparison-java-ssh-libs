package com.github.sparsick.ssh4j;

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.local.LocalFile;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class VfsSftpClient implements SshClient {
    private String user;
    private String password;
    private Path privateKey;
    private Path knownHosts;
    private boolean useCompression = false;
    private StandardFileSystemManager fileSystemManager;
    private FileObject remoteRootDirectory;

    @Override
    public void authUserPassword(String user, String password) {
        this.user = user;
        this.password = password;
    }

    @Override
    public void authUserPublicKey(String user, Path privateKey) {
        this.user = user;
        this.privateKey = privateKey;
    }

    @Override
    public void setKnownHosts(Path knownHosts) {
        this.knownHosts = knownHosts;
    }

    @Override
    public void useCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }

    @Override
    public void connect(String host, int port) throws IOException {
        initFileSystemManager();
        FileSystemOptions connectionOptions = buildConnectionOptions();
        String connectionUrl = buildConnectionUrl(host, port);
        remoteRootDirectory = fileSystemManager.resolveFile(connectionUrl, connectionOptions);
    }

    @Override
    public void connect(String host) throws IOException {
        connect(host, 22);
    }

    private void initFileSystemManager() throws FileSystemException {
        if (fileSystemManager == null) {
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
            fileSystemManager = new StandardFileSystemManager();
            fileSystemManager.init();
        }
    }

    private FileSystemOptions buildConnectionOptions() throws FileSystemException {
        SftpFileSystemConfigBuilder sftpConfigBuilder = SftpFileSystemConfigBuilder.getInstance();
        FileSystemOptions opts = new FileSystemOptions();
        sftpConfigBuilder.setUserDirIsRoot(opts, false);
        if (knownHosts != null) {
            sftpConfigBuilder.setStrictHostKeyChecking(opts, "yes");
            sftpConfigBuilder.setKnownHosts(opts, knownHosts.toFile());
        } else {
            sftpConfigBuilder.setStrictHostKeyChecking(opts, "no");
//            sftpConfigBuilder.setKnownHosts(opts, new File("~/.ssh/known_hosts"));
        }

        if (useCompression) {
            sftpConfigBuilder.setCompression(opts, "zlib@openssh.com,zlib,none");
        }

        if (privateKey != null) {
            sftpConfigBuilder.setIdentities(opts, new File[]{privateKey.toFile()});
        }
        return opts;
    }

    private String buildConnectionUrl(String host, int port) {
        if (privateKey != null) {
            return String.format("sftp://%s@%s:%d", user, host, port);
        } else if (password != null) {
            return String.format("sftp://%s:%s@%s:%d", user, password, host, port);
        } else {
            throw new RuntimeException("Either privateKey nor password is set. Please call one of the auth methods.");
        }
    }

    @Override
    public void disconnect() {
        if (fileSystemManager != null) {
            fileSystemManager.close();
            fileSystemManager = null;
        }
    }

    @Override
    public void download(String remotePath, Path local) throws IOException {
        LocalFile localFileObject = (LocalFile) fileSystemManager.resolveFile(local.toUri().toString());
        FileObject remoteFileObject = remoteRootDirectory.resolveFile(remotePath);
        try {
            localFileObject.copyFrom(remoteFileObject, new AllFileSelector());
        } finally {
            localFileObject.close();
            remoteFileObject.close();
        }

    }

    @Override
    public void upload(Path local, String remotePath) throws IOException {
        LocalFile localFileObject = (LocalFile) fileSystemManager.resolveFile(local.toUri().toString());
        FileObject remoteFileObject = remoteRootDirectory.resolveFile(remotePath);
        try {
            remoteFileObject.copyFrom(localFileObject, new AllFileSelector());
        } finally {
            localFileObject.close();
            remoteFileObject.close();
        }
    }

    @Override
    public void move(String oldRemotePath, String newRemotePath) throws IOException {
        FileObject remoteOldFileObject = remoteRootDirectory.resolveFile(oldRemotePath);
        FileObject newRemoteFileObject = remoteRootDirectory.resolveFile(newRemotePath);
        try {
            remoteOldFileObject.moveTo(newRemoteFileObject);
            remoteOldFileObject.close();
        } finally {
            newRemoteFileObject.close();
        }
    }

    @Override
    public void copy(String oldRemotePath, String newRemotePath) throws IOException {
        FileObject newRemoteFileObject = remoteRootDirectory.resolveFile(newRemotePath);
        FileObject oldRemoteFileObject = remoteRootDirectory.resolveFile(oldRemotePath);
        try {
            newRemoteFileObject.copyFrom(oldRemoteFileObject, new AllFileSelector());
        } finally {
            oldRemoteFileObject.close();
            newRemoteFileObject.close();
        }
    }

    @Override
    public void delete(String remotePath) throws IOException {
        FileObject remoteFileObject = remoteRootDirectory.resolveFile(remotePath);
        try {
            remoteFileObject.delete();
        } finally {
            remoteFileObject.close();
        }
    }

    @Override
    public boolean fileExists(String remotePath) throws IOException {
        FileObject remoteFileObject = remoteRootDirectory.resolveFile(remotePath);
        try {
            return remoteFileObject.exists();
        } finally {
            remoteFileObject.close();
        }
    }

    @Override
    public List<String> listChildrenNames(String remotePath) throws IOException {
        return listChildrenNamesByFileType(remotePath, null);
    }

    @Override
    public List<String> listChildrenFolderNames(String remotePath) throws IOException {
        return listChildrenNamesByFileType(remotePath, FileType.FOLDER);
    }

    @Override
    public List<String> listChildrenFileNames(String remotePath) throws IOException {
        return listChildrenNamesByFileType(remotePath, FileType.FILE);
    }

    private List<String> listChildrenNamesByFileType(String remotePath, FileType fileType) throws FileSystemException {
        FileObject remoteFileObject = remoteRootDirectory.resolveFile(remotePath);
        try {
            FileObject[] fileObjectChildren = remoteFileObject.getChildren();
            List<String> childrenNames = new ArrayList<>();
            for (FileObject child : fileObjectChildren) {
                if (fileType == null || child.getType() == fileType) {
                    childrenNames.add(child.getName().getBaseName());
                }
            }
            return childrenNames;
        } finally {
            remoteFileObject.close();
        }
    }

    @Override
    public void execute(String command) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() throws Exception {
        disconnect();
    }

}