package ch.autumo.beetroot.server.communication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.server.message.ClientAnswer;
import ch.autumo.beetroot.server.message.HealthAnswer;
import ch.autumo.beetroot.server.message.ServerCommand;
import ch.autumo.beetroot.server.message.StopAnswer;
import ch.autumo.beetroot.transport.DefaultSocketFactory;
import ch.autumo.beetroot.transport.SecureSocketFactory;
import ch.autumo.beetroot.transport.SocketFactory;
import ch.autumo.beetroot.utils.security.SSLUtils;

public class ClientCommunicator extends Communicator {

	private static SocketFactory socketFactory = null;
	
	private static int clientTimeout = -1;
	private static boolean sslSockets = false;
	
	static {
		reInit();
	}
	
	/**
	 * Re-initialize client communicator.
	 */
	public static void reInit() {
		// read some undocumented settings if available
		clientTimeout = BeetRootConfigurationManager.getInstance().getIntNoWarn("client_timeout"); // in ms !
		// SSL sockets?
		final String mode = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_COM_ENC);
		sslSockets = (mode != null && mode.equalsIgnoreCase("ssl"));
		
		if (sslSockets) {
			try {
		        socketFactory = new SecureSocketFactory(SSLUtils.makeSSLSocketFactory(SSLUtils.getKeystoreFile(), SSLUtils.getKeystorePw()), null);
			} catch (Exception e) {
				LOG.error("Cannot make client calls secure (SSL)! ", e);
				System.err.println("Cannot make client calls secure (SSL)! ");
			}
		} else {
			socketFactory = new DefaultSocketFactory();
		}		
	}
	
	
	// Client-side
	//------------------------------------------------------------------------------
	
	/**
	 * Send a server command client side.
	 * 
	 * @param command server command
	 * @return client answer
	 * @throws Excpetion
	 */
	public static ClientAnswer sendServerCommand(ServerCommand command) throws Exception {
		
		//send signal and end !
		Socket socket = null;
		DataOutputStream output = null;
		DataInputStream input = null;
		int timeout = command.getTimeout();
		try {
			if (clientTimeout > 0)
				timeout = clientTimeout;
				
			socket = socketFactory.create(command.getHost(), command.getPort());
			socket.setSoTimeout(timeout);
			output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

			output.writeInt(command.getDataLength());
			final PrintWriter writer = new PrintWriter(output, true);
			writer.println(command.getTransferString());
			
			LOG.trace("Server command '"+command.getCommand()+"' sent!");
			writer.flush();
			
			if (command.getCommand().equals(CMD_STOP)) {
				// we cannot expect an answer, because the server is already down
				return new StopAnswer();
			} else if (command.getCommand().equals(CMD_HEALTH)) {
				return new HealthAnswer();
			} else {
				// read answer
				input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
				return readAnswer(input);
			}
		} catch (UnknownHostException e) {
			LOG.error(command.getServerName() + " admin server cannot be contacted at "+command.getHost()+":"+command.getPort()+"! Host seems to be unknown or cannot be resolved. [UHE]", e);
			throw e;
		} catch (IOException e) {
			LOG.error(command.getServerName() + " admin server cannot be contacted at "+command.getHost()+":"+command.getPort()+"! PS: Is it really running? [IO]", e);
			throw e;
		} finally {
			safeClose(input);
			safeClose(output);
			safeClose(socket);
		}
	}

	/**
	 * Read an answer from the server client side.
	 * 
	 * @param in input stream
	 * @return client answer or null, if answer received was invalid
	 * @throws IOException
	 */
	public static ClientAnswer readAnswer(DataInputStream in) throws IOException {
	    return ClientAnswer.parse(read(in));
	}
	
}
