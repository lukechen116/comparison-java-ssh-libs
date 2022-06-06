package com.github.sparsick.ssh4j;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class JSchClient implements SshClient {
    private String password;
    private String user;
    private Path privateKey;
    private boolean useCompression = false;
    private Session session;
    private Path knownHosts;

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
        if (session == null) {
            try {
                JSch sshClient = new JSch();
                if (privateKey != null) {
                    sshClient.addIdentity(privateKey.toString());
                }
                session = sshClient.getSession(user, host, port);
                if (knownHosts != null) {
                    sshClient.setKnownHosts(knownHosts.toString());
                } else {
                    session.setConfig("StrictHostKeyChecking", "no");
//                    sshClient.setKnownHosts("~/.ssh/known_hosts");
                }

                if (useCompression) {
                    session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");
                    session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
                }

                if (password != null) {
                    session.setPassword(password);
                } else if (privateKey == null) {
                    throw new IOException("Either privateKey nor password is set. Please call one of the authentication method.");
                }
                session.connect();
            } catch (JSchException ex) {
                throw new IOException(ex);
            }
        }
    }

    @Override
    public void connect(String host) throws IOException {
        connect(host, 22);
    }

    @Override
    public void disconnect() {
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }

    @Override
    public void download(String remotePath, Path local) throws IOException {
        ChannelSftp sftpChannel = null;
        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            InputStream inputStream = sftpChannel.get(remotePath);
            Files.copy(inputStream, local);
        } catch (SftpException | JSchException ex) {
            throw new IOException(ex);
        } finally {
            if (sftpChannel != null) {
                sftpChannel.disconnect();
            }
        }
    }

    @Override
    public void upload(Path local, String remotePath) throws IOException {
        ChannelSftp sftpChannel = null;
        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            OutputStream outputStream = sftpChannel.put(remotePath);
            Files.copy(local, outputStream);
        } catch (SftpException | JSchException ex) {
            throw new IOException(ex);
        } finally {
            if (sftpChannel != null) {
                sftpChannel.disconnect();
            }
        }
    }

    @Override
    public void move(String oldRemotePath, String newRemotePath) throws IOException {
        ChannelSftp sftpChannel = null;
        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            sftpChannel.rename(oldRemotePath, newRemotePath);
        } catch (SftpException | JSchException ex) {
            throw new IOException(ex);
        } finally {
            if (sftpChannel != null) {
                sftpChannel.disconnect();
            }
        }
    }

    @Override
    public void copy(String oldRemotePath, String newRemotePath) throws IOException {
        executeCommand("cp " + oldRemotePath + " " + newRemotePath);
    }

    private void executeCommand(String command) throws IOException {
        ChannelExec execChannel = null;
        try {
            execChannel = (ChannelExec) session.openChannel("exec");
            execChannel.connect();
            execChannel.setCommand(command);
            execChannel.start();
        } catch (JSchException ex) {
            throw new IOException(ex);
        } finally {
            if (execChannel != null) {
                execChannel.disconnect();

            }
        }
    }

    @Override
    public void delete(String remotePath) throws IOException {
        ChannelSftp sftpChannel = null;
        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            sftpChannel.rm(remotePath);
        } catch (SftpException | JSchException ex) {
            throw new IOException(ex);
        } finally {
            if (sftpChannel != null) {
                sftpChannel.disconnect();

            }
        }
    }

    @Override
    public boolean fileExists(String remotePath) throws IOException {
        ChannelSftp sftpChannel = null;
        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            sftpChannel.ls(remotePath);
            return true;
        } catch (JSchException ex) {
            throw new IOException(ex);
        } catch (SftpException ex) {
            return false;
        } finally {
            if (sftpChannel != null) {
                sftpChannel.disconnect();

            }
        }
    }

    @Override
    public List<String> listChildrenNames(String remotePath) throws IOException {
        ChannelSftp sftpChannel = null;
        List<String> children = new ArrayList<>();
        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            Vector<ChannelSftp.LsEntry> ls = sftpChannel.ls(remotePath);
            for (ChannelSftp.LsEntry l : ls) {
                children.add(l.getFilename());
            }
            return children;
        } catch (SftpException | JSchException ex) {
            throw new IOException(ex);
        } finally {
            if (sftpChannel != null) {
                sftpChannel.disconnect();
            }
        }
    }

    @Override
    public List<String> listChildrenFolderNames(String remotePath) throws IOException {
        ChannelSftp sftpChannel = null;
        List<String> folderChildren = new ArrayList<>();
        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            sftpChannel.ls(remotePath, (ChannelSftp.LsEntry entry) -> {
                if (entry.getAttrs().isDir()) {
                    folderChildren.add(entry.getFilename());
                }
                return ChannelSftp.LsEntrySelector.CONTINUE;
            });
            return folderChildren;
        } catch (SftpException | JSchException ex) {
            throw new IOException(ex);
        } finally {
            if (sftpChannel != null) {
                sftpChannel.disconnect();

            }
        }
    }

    @Override
    public List<String> listChildrenFileNames(String remotePath) throws IOException {
        ChannelSftp sftpChannel = null;
        List<String> folderChildren = new ArrayList<>();
        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            sftpChannel.ls(remotePath, (ChannelSftp.LsEntry entry) -> {
                if (entry.getAttrs().isReg()) {
                    folderChildren.add(entry.getFilename());
                }
                return ChannelSftp.LsEntrySelector.CONTINUE;
            });
            return folderChildren;
        } catch (SftpException | JSchException ex) {
            throw new IOException(ex);
        } finally {
            if (sftpChannel != null) {
                sftpChannel.disconnect();

            }
        }
    }

    @Override
    public void execute(String command) throws IOException {
        executeCommand(command);
    }

    @Override
    public void close() throws Exception {
        disconnect();
    }
}