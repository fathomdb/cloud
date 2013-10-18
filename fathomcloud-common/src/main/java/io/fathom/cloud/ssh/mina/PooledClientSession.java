package io.fathom.cloud.ssh.mina;

//
//import java.io.IOException;
//import java.security.KeyPair;
//import java.util.Map;
//
//import org.apache.sshd.ClientChannel;
//import org.apache.sshd.ClientSession;
//import org.apache.sshd.client.SshdSocketAddress;
//import org.apache.sshd.client.channel.ChannelDirectTcpip;
//import org.apache.sshd.client.channel.ChannelExec;
//import org.apache.sshd.client.channel.ChannelShell;
//import org.apache.sshd.client.channel.ChannelSubsystem;
//import org.apache.sshd.client.future.AuthFuture;
//import org.apache.sshd.common.future.CloseFuture;
//
//import io.fathom.cloud.ssh.SshClientPool;
//
//public class PooledClientSession implements ClientSession {
//	final SshClientPool pool;
//	ClientSession inner;
//
//	public PooledClientSession(SshClientPool pool, ClientSession inner) {
//		super();
//		this.pool = pool;
//		this.inner = inner;
//	}
//
//	@Override
//	public AuthFuture authAgent(String username) throws IOException {
//		return inner.authAgent(username);
//	}
//
//	@Override
//	public AuthFuture authPassword(String username, String password)
//			throws IOException {
//		return inner.authPassword(username, password);
//	}
//
//	@Override
//	public AuthFuture authPublicKey(String username, KeyPair key)
//			throws IOException {
//		return inner.authPublicKey(username, key);
//	}
//
//	@Override
//	public ClientChannel createChannel(String type) throws Exception {
//		return inner.createChannel(type);
//	}
//
//	@Override
//	public ClientChannel createChannel(String type, String subType)
//			throws Exception {
//		return inner.createChannel(type, subType);
//	}
//
//	@Override
//	public ChannelShell createShellChannel() throws Exception {
//		return inner.createShellChannel();
//	}
//
//	@Override
//	public ChannelExec createExecChannel(String command) throws Exception {
//		return inner.createExecChannel(command);
//	}
//
//	@Override
//	public ChannelSubsystem createSubsystemChannel(String subsystem)
//			throws Exception {
//		return inner.createSubsystemChannel(subsystem);
//	}
//
//	@Override
//	public ChannelDirectTcpip createDirectTcpipChannel(SshdSocketAddress local,
//			SshdSocketAddress remote) throws Exception {
//		if (inner == null) {
//			throw new IllegalStateException();
//		}
//		return inner.createDirectTcpipChannel(local, remote);
//	}
//
//	@Override
//	public void startLocalPortForwarding(SshdSocketAddress local,
//			SshdSocketAddress remote) throws Exception {
//		inner.startLocalPortForwarding(local, remote);
//	}
//
//	@Override
//	public void stopLocalPortForwarding(SshdSocketAddress local)
//			throws Exception {
//		inner.stopLocalPortForwarding(local);
//	}
//
//	@Override
//	public void startRemotePortForwarding(SshdSocketAddress remote,
//			SshdSocketAddress local) throws Exception {
//		inner.startRemotePortForwarding(remote, local);
//	}
//
//	@Override
//	public void stopRemotePortForwarding(SshdSocketAddress remote)
//			throws Exception {
//		inner.stopRemotePortForwarding(remote);
//	}
//
//	@Override
//	public int waitFor(int mask, long timeout) {
//		return inner.waitFor(mask, timeout);
//	}
//
//	@Override
//	public CloseFuture close(boolean immediately) {
//		CloseFuture future = pool.returnToPool(immediately, this);
//		inner = null;
//		return future;
//	}
//
//	@Override
//	public Map<Object, Object> getMetadataMap() {
//		return inner.getMetadataMap();
//	}
//
// }
