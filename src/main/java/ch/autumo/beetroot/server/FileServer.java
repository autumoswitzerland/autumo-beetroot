/**
 * 
 * Copyright (c) 2023 autumo Ltd. Switzerland, Michael Gasche
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package ch.autumo.beetroot.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.server.action.Download;
import ch.autumo.beetroot.server.action.Upload;
import ch.autumo.beetroot.server.communication.Communicator;
import ch.autumo.beetroot.server.communication.FileTransfer;
import ch.autumo.beetroot.server.message.ClientAnswer;
import ch.autumo.beetroot.server.message.ServerCommand;
import ch.autumo.beetroot.server.message.file.FileAnswer;
import ch.autumo.beetroot.server.message.file.PingUploadRequest;
import ch.autumo.beetroot.server.modules.FileStorage;
import ch.autumo.beetroot.utils.UtilsException;


/**
 * File server.
 */
public class FileServer {

	protected static final Logger LOG = LoggerFactory.getLogger(FileServer.class.getName());
	
	/** the base server */
	private BaseServer baseServer = null;
	
	protected int portFileServer = -1;
	protected int portFileReceiver = -1;
	private ServerSocket fileServerSocket = null;
	private ServerSocket fileReceiverSocket = null;
	private int serverTimeout = -1;
	
	private FileStorage fileStorage = null;
	
	private boolean sslSockets = false;
	private boolean stopped = false;
	
	
	/** The download queue */
	private List<Download> downloadQueue = Collections.synchronizedList(new ArrayList<Download>());
	
	/** The upload queue */
	private List<Upload> uploadQueue = Collections.synchronizedList(new ArrayList<Upload>());
	
	
	/**
	 * The file server.
	 * 
	 * @param baseServer base server
	 * @param fileStorage file storage
	 */
	public FileServer(BaseServer baseServer, FileStorage fileStorage) {
		this.baseServer = baseServer;
		this.fileStorage = fileStorage;
		portFileServer = BeetRootConfigurationManager.getInstance().getInt(Constants.KEY_ADMIN_FILE_PORT);
		if (portFileServer == -1) {
			LOG.error("File server port not specified! Using port '{}'.", FileTransfer.DEFAULT_FILE_SERVER_PORT);
			portFileServer = FileTransfer.DEFAULT_FILE_SERVER_PORT;
		}
		portFileReceiver = BeetRootConfigurationManager.getInstance().getInt(Constants.KEY_ADMIN_FILE_RECEIVER_PORT);
		if (portFileReceiver == -1) {
			LOG.error("File receiver port not specified! Using port '{}'.", FileTransfer.DEFAULT_FILE_RECEIVER_PORT);
			portFileReceiver = FileTransfer.DEFAULT_FILE_RECEIVER_PORT;
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
		final FileServerListener fileListener = new FileServerListener(portFileServer);
		final Thread flServer = new Thread(fileListener);
		flServer.setName(baseServer.name + "-FileServer");
		flServer.start();
		final FileReceiverListener fileReceiverListener = new FileReceiverListener(portFileReceiver);
		final Thread frServer = new Thread(fileReceiverListener);
		frServer.setName(baseServer.name + "-FileReceiverServer");
		frServer.start();
	}

	/**
	 * Stop file server.
	 */
	public void stop() {
		stopped = true;
		new Thread(() -> {
			Communicator.safeClose(fileServerSocket);
		}).start();
		new Thread(() -> {
			Communicator.safeClose(fileReceiverSocket);
		}).start();
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
	 * Queue upload for client that requested to receive 
	 * a file server-side
	 * 
	 * @param upload upload
	 */
	public void addToUploadQueue(Upload upload) {
		uploadQueue.add(upload);
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
	
	
	// File Server
	//------------------------------------------------------------------------------
	
	/**
	 * File server listener.
	 */
	private final class FileServerListener implements Runnable {
	
		private ExecutorService clientExecutorService = Executors.newCachedThreadPool();
		private int listenerPort = -1;

		/**
		 * Create file listener on specific port.
		 * 
		 * @param listenerPort listener port
		 */
		public FileServerListener(int listenerPort) {
			this.listenerPort = listenerPort;
			// Communication is encrypted through the command message (cmd),
			// by SSL sockets (ssl) or it is not (none) 
			try {
				fileServerSocket = baseServer.serverSocketFactory.create(this.listenerPort);
				if (serverTimeout > 0) // shouldn't be set, should be endless, just for testing purposes
					fileServerSocket.setSoTimeout(serverTimeout);
					
			} catch (IOException e) {
				LOG.error("File server listener cannot be created on port '" + this.listenerPort + "'!", e);
				System.err.println(BaseServer.ansiErrServerName + " File server listener cannot be created on port '" + this.listenerPort + "'!");
			}
		}	
		
		@Override
		public void run() {
			// File server main loop
			while (!stopped) {
				Socket clientSocket = null;
				try {
					// it waits for a connection
					clientSocket = fileServerSocket.accept();
					final String threadName;
					final InetAddress addr = clientSocket.getInetAddress();
					if (addr != null) {
						threadName = FileServer.this.baseServer.name + "-FileServerClient(" + addr.toString() + ")";
					} else {
						threadName = FileServer.this.baseServer.name + "-FileServerClient";
					}
					if (clientSocket != null) {
						final ClientFileHandler handler = new ClientFileHandler(clientSocket);
                        clientExecutorService.execute(() -> {
                            Thread.currentThread().setName(threadName);
                            try {
                            	handler.run();								
							} catch (Exception e) {
								LOG.error("Error handling client: " + threadName, e);
				                throw new RuntimeException("Error handling client: " + threadName, e);
							}
                        });							
					}
					
		        } catch (IOException e) {
		        	if (!stopped) {
		        		LOG.error("File server connection listener failed! We recommend to restart the server!", e);
		        	}
		        } finally {
		        	if (!stopped && fileServerSocket != null && fileServerSocket.isClosed()) {
	        			try {
	        				fileServerSocket = baseServer.serverSocketFactory.create(this.listenerPort);
		    				if (serverTimeout > 0)
		    					fileServerSocket.setSoTimeout(serverTimeout);
	        			} catch (IOException e) {
	        				LOG.error("Failed to recreate server socket after it was closed.", e);
	        			}
		        	}
	            }				
            } 
		
			// loop has been broken by STOP command.
			Communicator.safeClose(fileServerSocket);
			// shutdown thread pool
			clientExecutorService.shutdown();
		    try {
		        // Wait for tasks to complete for up to 60 seconds
		        if (!clientExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
		            clientExecutorService.shutdownNow(); // Force shutdown if timeout occurs
		            if (!clientExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
		                LOG.error("Client executor service did not terminate.");
		            }
		        }
		    } catch (InterruptedException ie) {
		        // If interrupted, force shutdown immediately
		        clientExecutorService.shutdownNow();
		        Thread.currentThread().interrupt(); // Restore interrupt status
		    }			
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
				command = Communicator.readCommand(in);
			
				// Security checks:
				// 0. invalid server command?
				if (command == null) {
					LOG.error("Server command: received command is too long, command is ignored!");
					return;
				}
				// 1. correct server name?
				final String serverName = command.getServerName();
				if (!serverName.equals(FileServer.this.baseServer.name)) {
					LOG.error("Server command: Wrong server name received, command is ignored!");
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
			        	Communicator.safeClose(out);
					}			
				}
	        } catch (UtilsException e) {
				LOG.error("File server couldn't decode server command from a client; someone or something is sending false messages!");
				LOG.error("  -> Either the secret key seed doesn't match on both sides ('msg' mode) or");
				LOG.error("     different encrypt modes have been defined on boths side, or the server's");
				LOG.error("     configuration is set to encode server-client communication, but the client's isn't!");
				LOG.error("  -> Check config 'admin_com_encrypt' on both ends.");
	        }	
	        catch (IOException e) {
				LOG.error("File server listener failed! Possible invalid messages received.", e);
	        } finally {
				Communicator.safeClose(in);
	        	Communicator.safeClose(clientSocket);
	        }					
		}		
	}
	
	
	// File Receiver
	//------------------------------------------------------------------------------
	
	/**
	 * File receiver listener.
	 */
	private final class FileReceiverListener implements Runnable {
	
		private ExecutorService clientExecutorService = Executors.newCachedThreadPool();
		private int listenerPort = -1;

		/**
		 * Create file receiver listener on specific port.
		 * 
		 * @param listenerPort listener port
		 */
		public FileReceiverListener(int listenerPort) {
			this.listenerPort = listenerPort;
			// Communication is encrypted through the command message (cmd),
			// by SSL sockets (ssl) or it is not (none) 
			try {
				fileReceiverSocket = baseServer.serverSocketFactory.create(this.listenerPort);
				if (serverTimeout > 0) // shouldn't be set, should be endless, just for testing purposes
					fileReceiverSocket.setSoTimeout(serverTimeout);
			} catch (IOException e) {
				LOG.error("File receiver listener cannot be created on port '" + this.listenerPort + "'!", e);
				System.err.println(BaseServer.ansiErrServerName + " File receiver listener cannot be created on port '" + this.listenerPort + "'!");
			}
		}	
		
		@Override
		public void run() {
			while (!stopped) {
				Socket clientSocket = null;
				try {
					// it waits for a connection
					clientSocket = fileReceiverSocket.accept();
					final String threadName;
					InetAddress addr = clientSocket.getInetAddress();
					if (addr != null) {
						threadName = FileServer.this.baseServer.name + "-FileReceiverClient(" + addr.toString() + ")";
					} else {
						threadName = FileServer.this.baseServer.name + "-FileReceiverClient";
					}
					if (clientSocket != null) {
						final ClientReceiverHandler handler = new ClientReceiverHandler(clientSocket);
                        clientExecutorService.execute(() -> {
                            Thread.currentThread().setName(threadName);
                            try {
                            	handler.run();								
							} catch (Exception e) {
								LOG.error("Error handling client: " + threadName, e);
				                throw new RuntimeException("Error handling client: " + threadName, e);
							}
                        });							
					}					
		        } catch (IOException e) {
		        	if (!stopped) {
		        		LOG.error("File receiver connection listener failed! We recommend to restart the server!", e);
		        	}
		        } finally {
		        	if (!stopped) {
		        		if (fileReceiverSocket != null && fileReceiverSocket.isClosed()) {
		        			try {
		        				if (sslSockets) {
		        					final ServerSocketFactory socketFactory = SSLServerSocketFactory.getDefault();
		        					fileReceiverSocket = socketFactory.createServerSocket(this.listenerPort);
		        				} else {
		        					fileReceiverSocket = new ServerSocket(this.listenerPort);
		        				}
		        			} catch (IOException e) {
		        				// That's wild, I know 
		        			}
		        		}
		        	}
	            }				
            } 
			// loop has been broken by STOP command.
			Communicator.safeClose(fileReceiverSocket);
			// shutdown thread pool
			clientExecutorService.shutdown();
		    try {
		        // Wait for tasks to complete for up to 60 seconds
		        if (!clientExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
		            clientExecutorService.shutdownNow(); // Force shutdown if timeout occurs
		            if (!clientExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
		                LOG.error("Client executor service did not terminate.");
		            }
		        }
		    } catch (InterruptedException ie) {
		        // If interrupted, force shutdown immediately
		        clientExecutorService.shutdownNow();
		        Thread.currentThread().interrupt(); // Restore interrupt status
		    }				
		}		
	}

	/**
	 * Client receiver handler for every request.
	 */
	private final class ClientReceiverHandler implements Runnable {

		private Socket clientSocket = null;
		private DataInputStream in = null;
		
		/**
		 * Constructor.
		 * 
		 * @param clientSocket client socket
		 */
		private ClientReceiverHandler(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}
		
		@Override
		public void run() {
			File file = null;
			Upload upload = null;
			boolean verified = false;
			try {
				in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
				// Only files!
				final long size = in.readLong();
				for (Iterator<Upload> iterator = uploadQueue.iterator(); iterator.hasNext();) {
					upload = iterator.next();
					if (upload.getSize() == size) { // First check: file size
						file = FileTransfer.readFile(in, upload.getFileName(), size);
						final String absPath = file.getAbsolutePath();
						final Path path = Paths.get(absPath);
					    byte data[];
					    String checkSum = null;
						try {
							data = Files.readAllBytes(path);
						    byte hash[] = MessageDigest.getInstance("MD5").digest(data);
						    checkSum = new BigInteger(1, hash).toString(16);
						} catch (Exception e) {
							throw new IOException("Couldn't build checksum for file '" + absPath + "!", e);
						}						
						if (upload.getCheckSum().equals(checkSum)) {
							verified = true;
							uploadQueue.remove(upload);
							break;
						}
					}
				}
				// Do work and create answers!
				this.process(upload, file, verified);
	        } catch (UtilsException e) {
				LOG.error("File receiver couldn't decode server command from a client; someone or something is sending false messages!");
				LOG.error("  -> Either the secret key seed doesn't match on both sides ('msg' mode) or");
				LOG.error("     different encrypt modes have been defined on boths side, or the server's");
				LOG.error("     configuration is set to encode server-client communication, but the client's isn't!");
				LOG.error("  -> Check config 'admin_com_encrypt' on both ends.");
	        }
	        catch (IOException e) {
				LOG.error("File receiver listener failed! Possible invalid messages received.", e);
	        } finally {
				Communicator.safeClose(in);
	        	Communicator.safeClose(clientSocket);
				// Purge the temporary file if any!
				if (file != null && file.exists()) {
					file.delete();
				}
	        }
		}
		
		private void process(Upload upload, File file, boolean verified) {
			// 0. PING answer (for health status)
			if (upload.getFileName().startsWith(PingUploadRequest.PING_FILE_PREFIX)) {
				// Dummy answer!
				DataOutputStream out = null;
				try {
					out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
					Communicator.writeAnswer(new FileAnswer(upload.getFileName(), "PING"), out);
				} catch (IOException e) {
					LOG.error("Ping receiver client response failed! We recommend to restart the server!", e);
					System.err.println(BaseServer.ansiErrServerName + " Ping receiver client response failed! We recommend to restart the server!");
		        } finally {
		        	Communicator.safeClose(out);
				}			
			// 1. Store file and return state of file storage
			} else if (file != null && verified) {
				// store it !
				String uniqueFileId = null;
				try {
					if (fileStorage != null)
						uniqueFileId = fileStorage.store(file, upload.getFileName(), upload.getUser(), upload.getDomain());
					else
						uniqueFileId = baseServer.store(file, upload.getFileName(), upload.getUser(), upload.getDomain());
				} catch (Exception e1) {
					LOG.error("Couldn't store received file '"+upload.getFileName()+"'!", e1);
					System.err.println(BaseServer.ansiErrServerName + " Couldn't store received file '"+upload.getFileName()+"'!");
					uniqueFileId = null;
				}					
				DataOutputStream out = null;
				try {
					out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
					if (uniqueFileId != null)
						Communicator.writeAnswer(new FileAnswer(upload.getFileName(), uniqueFileId), out);
					else
						Communicator.writeAnswer(new FileAnswer(upload.getFileName(), ClientAnswer.TYPE_FILE_NOK), out);
					
				} catch (IOException e) {
					LOG.error("File receiver client response failed! We recommend to restart the server!", e);
					System.err.println(BaseServer.ansiErrServerName + " File receiver client response failed! We recommend to restart the server!");
		        } finally {
		        	Communicator.safeClose(out);
				}			
			// 2. No file received of file couldn't be verified
			} else {
				DataOutputStream out = null;
				try {
					out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
					Communicator.writeAnswer(new FileAnswer("No matching file upload request found in upload queue!", ClientAnswer.TYPE_FILE_NOK), out);
				} catch (IOException e) {
					LOG.error("File receiver client response failed! We recommend to restart the server!", e);
					System.err.println(BaseServer.ansiErrServerName + " File receiver client response failed! We recommend to restart the server!");
		        } finally {
		        	Communicator.safeClose(out);
				}							
			}			
		}
	}
	
}
