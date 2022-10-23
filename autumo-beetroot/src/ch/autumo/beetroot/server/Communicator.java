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
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;

/**
 * Client/Server communication
 */
public class Communicator {

	protected final static Logger LOG = LoggerFactory.getLogger(BaseServer.class.getName());
	
	/** Stop command */
	public final static String STOP_COMMAND = "STOP";

	/** Connection timeout in seconds */
	public final static int TIMEOUT = 5;
	
	private static int clientTimeout = -1;
	static {
		// read some undocumented settings if available
		clientTimeout = BeetRootConfigurationManager.getInstance().getInt("client_timeout"); // in ms !
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
		return sendServerCommand(command, TIMEOUT * 1000);
	}
	
	/**
	 * Send a server command client side.
	 * 
	 * @param command server command
	 * @param command timeout socket timeout in milliseconds
	 * @return client answer
	 * @throws Excpetion
	 */
	public static ClientAnswer sendServerCommand(ServerCommand command, int timeout) throws Exception {
		
		//send signal and end !
		Socket socket = null;
		DataOutputStream output = null;
		DataInputStream input = null;
		try {
			
			if (clientTimeout > 0) {
				
				socket = new Socket(); 
				final SocketAddress socketAddress = new InetSocketAddress(command.getHost(), command.getPort()); 
				socket.connect(socketAddress, clientTimeout);
				
			} else {
				
				socket = new Socket(command.getHost(), command.getPort());
			}
			
			output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			socket.setSoTimeout(timeout);

			output.writeInt(command.getDataLength());
			final PrintWriter writer = new PrintWriter(output, true);
			writer.println(command.getTransferString());
			
			LOG.trace("Server command '"+command.getCommand()+"' sent!");
			writer.flush();
			
			if (command.getCommand().equals(STOP_COMMAND)) {
				// we cannot expect an answer, because the server is already down
				return new StopAnswer();
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

	
	
	// Server-side
	//------------------------------------------------------------------------------
	
	/**
	 * Write/send client answer from server to client.
	 * 
	 * @param client answer
	 * @param out output stream
	 * @throws Excpetion
	 */
	protected static void writeAnswer(ClientAnswer answer, DataOutputStream out) throws IOException {
		
		out.writeInt(answer.getDataLength());
		final PrintWriter writer = new PrintWriter(out, true);
		writer.println(answer.getTransferString());
		
		LOG.trace("Server command '"+answer.getAnswer()+"' sent!");
		writer.flush();			
	}
	
	/**
	 * Read a server command server side.
	 * 
	 * @param in input stream
	 * @return server command or null, if command received was invalid
	 * @throws IOException
	 */
	protected static ServerCommand readCommand(DataInputStream in) throws IOException {
	    return ServerCommand.parse(read(in));
	}
	
	/**
	 * Server side read.
	 * 
	 * @param in input stream
	 * @return unparsed data
	 * @throws IOException
	 */
	private static String read(DataInputStream in) throws IOException {
		
		final int length = in.readInt();
		
		if (length > 256) {
			// prevent other requests, max length should not be longer than 256 bytes
			return null; 
		}
		
		final byte[] messageByte = new byte[length];
	    boolean end = false;
	    final StringBuilder dataString = new StringBuilder(length);
	    int totalBytesRead = 0;
	    
	    while (!end) {
	    	
	        int currentBytesRead = in.read(messageByte);
	        totalBytesRead = currentBytesRead + totalBytesRead;
	        if(totalBytesRead <= length) {
	            dataString.append(new String(messageByte, 0, currentBytesRead, StandardCharsets.UTF_8));
	        } else {
	            dataString.append(new String(messageByte, 0, length - totalBytesRead + currentBytesRead, StandardCharsets.UTF_8));
	        }
	        
	        if (dataString.length() >= length) {
	            end = true;
	        }
	    }
	    
	    return dataString.toString();
	}	
	
	
	
	// Helper functions
	//------------------------------------------------------------------------------
	
	/**
	 * Safe close for closeable object  (e.g. stream, socket).
	 * 
	 * @param closeable closeable object
	 */
    protected static final void safeClose(Object closeable) {
        try {
            if (closeable != null) {
                if (closeable instanceof Closeable) {
                    ((Closeable) closeable).close();
                } else if (closeable instanceof Socket) {
                    ((Socket) closeable).close();
                } else if (closeable instanceof ServerSocket) {
                    ((ServerSocket) closeable).close();
                } else {
                    throw new IllegalArgumentException("Unknown object to close!");
                }
            }
        } catch (IOException e) {
            LOG.error("Could not close stream or socket!", e);
        }
    }
    
}
