/**
 * Copyright (c) 2022, autumo Ltd. Switzerland, Michael Gasche
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package ch.autumo.beetroot.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.utils.Utils;
import ch.autumo.beetroot.utils.UtilsException;


/**
 * File server.
 */
public class FileServer {

	protected final static Logger LOG = LoggerFactory.getLogger(FileServer.class.getName());
	
	/** the base server */
	private BaseServer baseServer = null;
	
	private FileListener fileListener = null;
	protected int portFileServer = -1;
	private ServerSocket serverSocket = null;
	private int serverTimeout = -1;
	
	private boolean sslSockets = false;

	private boolean stopped = false;
	
	
	/** The download queue */
	private List<Download> downloadQueue = Collections.synchronizedList(new ArrayList<Download>());
	
	
	/**
	 * The file server.
	 * 
	 * @param baseServer base server
	 */
	public FileServer(BaseServer baseServer) {
		
		this.baseServer = baseServer;
		
		portFileServer = BeetRootConfigurationManager.getInstance().getInt(Constants.KEY_ADMIN_FILE_PORT);
		if (portFileServer == -1) {
			LOG.error("File server port not specified! Using port '" + FileTransfer.DEFAULT_PORT + "'.");
			portFileServer = FileTransfer.DEFAULT_PORT;
		}
		
		// SSL sockets?
		final String mode = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_COM_ENC);
		sslSockets = (mode != null && mode.equalsIgnoreCase("ssl"));
		
		// read some undocumented settings if available
		serverTimeout = BeetRootConfigurationManager.getInstance().getIntNoWarn("server_timeout"); // in ms !
	}

	/**
	 * Start file server.
	 */
	public void start() {
		
		fileListener = new FileListener(portFileServer);
		final Thread server = new Thread(fileListener);
		server.setName(baseServer.name + "-FileServer");
		server.start();
	}

	/**
	 * Stop file server.
	 */
	public void stop() {
		stopped = true;
	}
	
	/**
	 * Queue download for client that requested a file.
	 * 
	 * @param download download
	 */
	public void addToDownloadQueue(Download download) {
		downloadQueue.add(download);
	}
	
	/**
	 * This method is called when a server command has been received.
	 * At this point security checks have been made.
	 * 
	 * @param command received server command
	 * @return download or null
	 */
	public Download processServerCommand(ServerCommand command) {
		
		// file request
		if (command.getCommand().equals(FileTransfer.CMD_FILE_GET)) {
			// all we need is the client to send back the unique file id
			final String uniqueFileId = command.getFileId();
			for (Iterator<Download> iterator = downloadQueue.iterator(); iterator.hasNext();) {
				final Download download = iterator.next();
				if (download.getFileId().equals(uniqueFileId)) {
					downloadQueue.remove(download);
					return download;
				}
			}
		}
		
		// nothing found
		return null;
	}
	
	/**
	 * File server listener.
	 */
	private final class FileListener implements Runnable {
	
		private int listenerPort = -1;

		/**
		 * Create file listener on specific port.
		 * 
		 * @param listenerPort listener port
		 */
		public FileListener(int listenerPort) {
			
			this.listenerPort = listenerPort;
			
			// Communication is encrypted through the command message (cmd),
			// by SSL sockets (ssl) or it is not (none) 
			try {
				if (sslSockets) {
					final ServerSocketFactory socketFactory = SSLServerSocketFactory.getDefault();
					serverSocket = socketFactory.createServerSocket(this.listenerPort);
				} else {
					serverSocket = new ServerSocket(this.listenerPort);
				}
				if (serverTimeout > 0) // shouldn't be set, should be endless, just for testing purposes
					serverSocket.setSoTimeout(serverTimeout);
					
			} catch (IOException e) {
				LOG.error("File server listener cannot be created on port '" + this.listenerPort + "'!", e);
				System.err.println(BaseServer.ansiErrServerName + " File server listener cannot be created on port '" + this.listenerPort + "'!");
				Utils.fatalExit();
			}
		}	
		
		@Override
		public void run() {
			
			while (!stopped) {
				
				Socket clientSocket = null;
				try {
					
					// it waits for a connection
					clientSocket = serverSocket.accept();
					if (clientSocket != null) {
						final ClientFileHandler handler = new ClientFileHandler(clientSocket);
						final Thread threadForClient = new Thread(handler);
						threadForClient.setName(FileServer.this.baseServer.name + "-FileClient");
						threadForClient.start();
					}
					
		        } catch (IOException e) {
		        	
		        	if (!stopped)
		        		LOG.error("File server connection listener failed! We recommend to restart the server!", e);
		        	
		        } finally {
		        	
		        	if (!stopped) {
		        		if (serverSocket != null && serverSocket.isClosed()) {
		        			try {
		        				if (sslSockets) {
		        					final ServerSocketFactory socketFactory = SSLServerSocketFactory.getDefault();
		        					serverSocket = socketFactory.createServerSocket(this.listenerPort);
		        				} else {
		        					serverSocket = new ServerSocket(this.listenerPort);
		        				}
		        			} catch (IOException e) {
		        				// That's wild, I know 
		        			}
		        		}
		        	}
	            }				
            } 
		
			// loop has been broken by STOP command.
			Communicator.safeClose(serverSocket);
		}		
	}
	
	/**
	 * Client file handler for every request.
	 */
	private final class ClientFileHandler implements Runnable {

		private Socket clientSocket = null;
		private DataInputStream in = null;
		
		/**
		 * Constructor.
		 * 
		 * @param clientSocket client socket
		 */
		private ClientFileHandler(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}
		
		@Override
		public void run() {
			
			ServerCommand command = null;
			try {
			
				in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));

				// Only file GET commands allowed!
				command = Communicator.readCommand(in);//XXX
	        } 
	        catch (UtilsException e) {
	        	
				LOG.error("File server couldn't decode server command from a client; someone or something is sending false messages!");
				LOG.error("  -> Either the secret key seed doesn't match on both sides ('msg' mode),");
				LOG.error("     different encrypt modes have beee defined on boths side, or the server's");
				LOG.error("     configuration is set to encode server-client communication, but the client's isn't!");
				LOG.error("  -> Check config 'admin_com_encrypt' on both ends.");
				return;
	        }	
	        catch (IOException e) {
	        	
				LOG.error("File server listener failed! We recommend to restart the server!", e);
				return;
	        }	
			
			
			// Security checks:
			// 0. invalid server command?
			if (command == null) {
				LOG.error("Server command: received command is too long, command is ignored!");
				Communicator.safeClose(in);
	        	Communicator.safeClose(clientSocket); //
				return;
			}
			// 1. correct server name?
			final String serverName = command.getServerName();
			if (!serverName.equals(FileServer.this.baseServer.name)) {
				LOG.error("Server command: Wrong server name received, command is ignored!");
				Communicator.safeClose(in);
	        	Communicator.safeClose(clientSocket); //
				return;
			}
			
			
			// execute command
			final Download download = FileServer.this.processServerCommand(command);
			
			// Deliver file!
			if (download != null) {

				DataOutputStream out = null;
				try {
	
					 out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
					 FileTransfer.writeFile(download, out);
				
				} catch (IOException e) {
		        	
					LOG.error("File server client response failed! We recommend to restart the server!", e);
					System.err.println(BaseServer.ansiErrServerName + " File server client response failed! We recommend to restart the server!");
					
		        } finally {
		        	
		        	Communicator.safeClose(in);
		        	Communicator.safeClose(out);
		        	Communicator.safeClose(clientSocket);
				}			
			}
		}		
	}
	
}
