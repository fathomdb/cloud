package io.fathom.cloud.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface SshDirectTcpipChannel extends SshChannel {

    InputStream getInputStream() throws IOException;

    OutputStream getOutputStream() throws IOException;

}
