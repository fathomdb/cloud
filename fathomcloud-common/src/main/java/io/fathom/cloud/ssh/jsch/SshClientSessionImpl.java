package io.fathom.cloud.ssh.jsch;

//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//
//import io.fathom.cloud.ssh.SshSession;
//import com.jcraft.jsch.Session;
//
//public class SshClientSessionImpl implements SshSession {
//	final Session inner;
//
//	public SshClientSessionImpl(Session inner) {
//		super();
//		this.inner = inner;
//	}
//
//	public ChannelDirectTcpip createDirectTcpipChannel(
//			InetSocketAddress tunnelLocal, InetSocketAddress tunnelRemote)
//			throws Exception {
//		SshdSocketAddress sshTunnelLocal = new SshdSocketAddress(tunnelLocal
//				.getAddress().getHostAddress(), tunnelLocal.getPort());
//		SshdSocketAddress sshTunnelRemote = new SshdSocketAddress(tunnelRemote
//				.getAddress().getHostAddress(), tunnelRemote.getPort());
//
//		return inner.createDirectTcpipChannel(sshTunnelLocal, sshTunnelRemote);
//
//	}
//
//	public void close() throws IOException {
//		boolean immediately = true;
//		CloseFuture closeFuture = inner.close(immediately);
//		try {
//			closeFuture.await();
//			// inner = null;
//		} catch (InterruptedException e) {
//			Thread.currentThread().interrupt();
//			throw new IOException("Error waiting for session close", e);
//		}
//	}
//
//	public void createSftp() {
//	}
//
// }
