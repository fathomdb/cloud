package io.fathom.cloud.ssh.mina;

//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.security.KeyPair;
//import java.util.Map;
//
//import io.fathom.cloud.ssh.SshClientPool;
//import io.fathom.cloud.ssh.SshContext;
//import com.google.common.collect.Maps;
//import com.jcraft.jsch.Session;
//
//public class SshContextImpl implements SshContext {
//
//	final SshClient sshClient;
//
//	final String sshUsername;
//
//	final KeyPair sshKey;
//
//	public SshContextImpl(SshClient sshClient, String sshUsername,
//			KeyPair sshKey) {
//		super();
//		this.sshClient = sshClient;
//		this.sshUsername = sshUsername;
//		this.sshKey = sshKey;
//	}
//
//	class ConnectionState implements SshClientPool {
//		final InetSocketAddress remote;
//
//		ClientSession session;
//
//		public ConnectionState(InetSocketAddress remote) {
//			super();
//			this.remote = remote;
//		}
//
//		public synchronized ClientSession getSession() throws IOException {
//			try {
//				if (session == null) {
//					Session session = jsch.getSession("user", server
//							.getAddress().getHostAddress(), server.getPort());
//
//					// session.setConfig("StrictHostKeyChecking", "no");
//
//					// session.setPassword("super_secre_password");
//					session.connect();
//
//					ConnectFuture connectFuture;
//					try {
//						connectFuture = sshClient.connect(remote);
//					} catch (Exception e) {
//						throw new IOException(
//								"Error connecting to SSH server: " + remote, e);
//					}
//					long connectTimeoutMillis = 60000;
//					if (!connectFuture.await(connectTimeoutMillis)) {
//						connectFuture.cancel();
//						throw new IOException(
//								"Timeout connecting to SSH server: " + remote);
//					}
//
//					ClientSession clientSession = connectFuture.getSession();
//
//					int ret = ClientSession.WAIT_AUTH;
//					while ((ret & ClientSession.WAIT_AUTH) != 0) {
//						int authTimeout = 30000;
//
//						clientSession.authPublicKey(sshUsername, sshKey);
//						ret = clientSession.waitFor(ClientSession.WAIT_AUTH
//								| ClientSession.CLOSED | ClientSession.AUTHED,
//								authTimeout);
//					}
//
//					if ((ret & ClientSession.CLOSED) != 0) {
//						throw new IOException(
//								"Unable to authenticate with SSH server: "
//										+ remote);
//					}
//
//					this.session = new PooledClientSession(this, clientSession);
//				}
//
//				useCount++;
//
//				return this.session;
//			} catch (InterruptedException e) {
//				Thread.currentThread().interrupt();
//				throw new IOException(
//						"Interrupted while connecting to SSH server: " + remote);
//			}
//		}
//
//		int useCount;
//
//		public synchronized void lock() {
//			useCount++;
//		}
//
//		public synchronized void release() {
//			useCount--;
//		}
//
//		@Override
//		public synchronized CloseFuture returnToPool(boolean immediately,
//				PooledClientSession inner) {
//			useCount--;
//			if (useCount <= 0) {
//				return inner.close(immediately);
//			} else {
//				CloseFuture future = new DefaultCloseFuture(null);
//				future.setClosed();
//				return future;
//			}
//		}
//	}
//
//	final Map<String, ConnectionState> connections = Maps.newHashMap();
//
//	ConnectionState getConnectionState(InetSocketAddress server) {
//		String key = server.getAddress().getHostAddress() + ":"
//				+ server.getPort();
//		ConnectionState connectionState = connections.get(key);
//		if (connectionState == null) {
//			connectionState = new ConnectionState(server);
//			connectionState.lock();
//			connections.put(key, connectionState);
//		}
//		return connectionState;
//	}
//
//	@Override
//	public SshSession getSession(InetSocketAddress server)
//			throws IOException {
//		ConnectionState connectionState = getConnectionState(server);
//		ClientSession session = connectionState.getSession();
//		return new SshSession(session);
//	}
//
//	@Override
//	public InetSocketAddress getRemoteSshAddress(InetSocketAddress address) {
//		int sshPort = 22;
//		return new InetSocketAddress(address.getAddress(), sshPort);
//	}
//
// }
