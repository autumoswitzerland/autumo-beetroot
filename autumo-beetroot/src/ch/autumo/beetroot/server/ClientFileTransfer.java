package ch.autumo.beetroot.server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.transport.DefaultSocketFactory;
import ch.autumo.beetroot.transport.SecureSocketFactory;
import ch.autumo.beetroot.transport.SocketFactory;
import ch.autumo.beetroot.utils.security.SSLUtils;

public class ClientFileTransfer extends FileTransfer {
	
	private static SocketFactory socketFactory = null;
	
	/** file server host */
	private static String hostAdmin = null;
	
	/** client timeout if configured */
	private static int clientTimeout = -1;
	
	/** use SSL sockets? */
	private static boolean sslSockets = false;

	/** file server port */
	protected static int portFileServer = -1;
	/** file receiver port (file-store end-point) */
	protected static int portFileReceiver = -1;
	
	static {
		reInit();
	}
	
	/**
	 * Re-initialize client file transfer.
	 */
	public static void reInit() {
		
		// File server port
		portFileServer = BeetRootConfigurationManager.getInstance().getInt(Constants.KEY_ADMIN_FILE_PORT);
		if (portFileServer == -1) {
			LOG.error("File server port not specified! Using port '" + DEFAULT_FILE_SERVER_PORT + "'.");
			portFileServer = DEFAULT_FILE_SERVER_PORT;
		}

		// File receiver port
		portFileReceiver = BeetRootConfigurationManager.getInstance().getInt(Constants.KEY_ADMIN_FILE_RECEIVER_PORT);
		if (portFileReceiver == -1) {
			LOG.error("File receiver port not specified! Using port '" + DEFAULT_FILE_RECEIVER_PORT + "'.");
			portFileReceiver = DEFAULT_FILE_RECEIVER_PORT;
		}
		
		// read some undocumented settings if available
		clientTimeout = BeetRootConfigurationManager.getInstance().getIntNoWarn("client_timeout"); // in ms !
		
		// File server host
		hostAdmin = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_HOST);
		
		// SSL sockets?
		final String mode = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_COM_ENC);
		sslSockets = (mode != null && mode.equalsIgnoreCase("ssl"));
		
		if (sslSockets) {
			boolean pwEncoded = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_PW_ENC); 
			String keystoreFile = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_KEYSTORE_FILE);
			try {
				final String keystorepw = pwEncoded ? 
						BeetRootConfigurationManager.getInstance().getDecodedString(Constants.KEY_KEYSTORE_PW, SecureApplicationHolder.getInstance().getSecApp()) : 
							BeetRootConfigurationManager.getInstance().getString(Constants.KEY_KEYSTORE_PW);
		        socketFactory = new SecureSocketFactory(SSLUtils.makeSSLSocketFactory(keystoreFile, keystorepw.toCharArray()), null);
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
	 * Get file.
	 * 
	 * @param fileId unique file ID
	 * @param filename file name
	 * @return file
	 * @throws Exception
	 */
	public static File getFile(String fileId, String filename) throws Exception {
		return getFile(fileId, filename, Communicator.TIMEOUT * 1000);
	}
	
	/**
	 * Get file.
	 * 
	 * @param fileId unique file ID
	 * @param filename file name
	 * @param timeout socket timeout
	 * @return file
	 * @throws Exception
	 */
	public static File getFile(String fileId, String filename, int timeout) throws Exception {
		
		//send signal and end !
		Socket socket = null;
		DataInputStream input = null;
		DataOutputStream output = null;
		File f = null;
		try {
			
			if (clientTimeout > 0)
				timeout = clientTimeout;
				
			socket = socketFactory.create(hostAdmin, portFileServer);
			socket.setSoTimeout(timeout);
			output = new DataOutputStream(socket.getOutputStream());

			final ServerCommand command = new ServerCommand(ServerCommand.DISPATCHER_ID_INTERNAL, FileTransfer.CMD_FILE_GET, fileId);
			output.writeInt(command.getDataLength());
			final PrintWriter writer = new PrintWriter(output, true);
			writer.println(command.getTransferString());
			
			LOG.trace("Server command '"+command.getCommand()+"' sent!");
			writer.flush();
			
			// read answer
			input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			
			final long size = input.readLong();
			f = readFile(input, filename, size);
			
		} catch (UnknownHostException e) {
			LOG.error("File server cannot be contacted at "+hostAdmin+":"+portFileServer+"! Host seems to be unknown or cannot be resolved. [UHE]", e);
			throw e;
		} catch (IOException e) {
			LOG.error("File server cannot be contacted at "+hostAdmin+":"+portFileServer+"! PS: Is it really running? [IO]", e);
			throw e;
		} finally {
			Communicator.safeClose(input);
			Communicator.safeClose(output);
			Communicator.safeClose(socket);
		}
		return f;
	}
	
	/**
	 * Send a file client side - a file store must be available server side.
	 * 
	 * @param file file
	 * @return client answer
	 * @throws Excpetion
	 */
	public static ClientAnswer sendFile(File file) throws Exception {
		return sendFile(file, Communicator.TIMEOUT * 1000);
	}
	
	/**
	 * Send a file client side - a file store must be available server side.
	 * 
	 * @param file server file
	 * @param command timeout socket timeout in milliseconds
	 * @return file answer
	 * @throws Excpetion
	 */
	public static FileAnswer sendFile(File file, int timeout) throws Exception {
		
		//send signal and end !
		Socket socket = null;
		DataOutputStream output = null;
		DataInputStream input = null;
		try {
			
			if (clientTimeout > 0)
				timeout = clientTimeout;
				
			socket = socketFactory.create(hostAdmin, portFileReceiver);
			output = new DataOutputStream(socket.getOutputStream());
			socket.setSoTimeout(timeout);

			final FileInputStream fileInputStream = new FileInputStream(file);
			
	        // send file size
			output.writeLong(file.length());  
	        // break file into chunks
	        final byte buffer[] = new byte[bufferLenKb];
	        int bytes = 0;
	        while ((bytes = fileInputStream.read(buffer)) != -1) {
	        	output.write(buffer, 0, bytes);
	        	output.flush();
	        }
	        fileInputStream.close();			
			
			LOG.trace("File '" + file.getName() + "' sent!");
			
			// read file answer
			input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			return ClientFileTransfer.readAnswer(input);
			
		} catch (UnknownHostException e) {
			
			LOG.error("File receiver cannot be contacted at "+hostAdmin+":"+portFileReceiver+"! Host seems to be unknown or cannot be resolved. [UHE]", e);
			throw e;
			
		} catch (IOException e) {
			
			LOG.error("File receiver cannot be contacted at "+hostAdmin+":"+portFileReceiver+"! PS: Is it really running? [IO]", e);
			throw e;
			
		} finally {
			
			Communicator.safeClose(input);
			Communicator.safeClose(output);
			Communicator.safeClose(socket);
		}
	}
	
	/**
	 * Read a file answer from the server client side when it received a file.
	 * Server must answer with a file answer when it has received a file.
	 * 
	 * @param in input stream
	 * @return file answer or null, if answer received was invalid
	 * @throws IOException
	 */
	public static FileAnswer readAnswer(DataInputStream in) throws IOException {
	    return (FileAnswer) FileAnswer.parse(Communicator.read(in));
	}		
}
