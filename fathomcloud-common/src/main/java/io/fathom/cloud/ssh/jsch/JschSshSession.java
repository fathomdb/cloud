package io.fathom.cloud.ssh.jsch;

//
//import java.io.IOException;
//
//import io.fathom.cloud.ssh.SftpChannel;
//import io.fathom.cloud.ssh.SshSession;
//import com.jcraft.jsch.ChannelSftp;
//import com.jcraft.jsch.JSchException;
//import com.jcraft.jsch.Session;
//
//public class JschSshSession implements SshSession {
//
//	final Session session;
//
//	@Override
//	public SftpChannel openSftpChannel() throws IOException {
//		ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
//		try {
//			sftpChannel.connect();
//		} catch (JSchException e) {
//			throw new IOException("Error opening sftp channel", e);
//		}
//		return new JschSftpChannel(sftpChannel);
//	}
//
// }
