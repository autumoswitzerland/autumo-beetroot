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
package ch.autumo.beetroot.server.communication;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.server.BaseServer;
import ch.autumo.beetroot.server.message.ClientAnswer;
import ch.autumo.beetroot.server.message.ServerCommand;

/**
 * Client/Server communication.
 */
public class Communicator {

	protected final static Logger LOG = LoggerFactory.getLogger(BaseServer.class.getName());
	
	/** Connection timeout in seconds */
	public final static int TIMEOUT = 5;
	
	/** Stop command */
	public final static String CMD_STOP = "STOP";
	/** Health command */
	public final static String CMD_HEALTH = "HEALTH";
	/** File delete */
	public final static String CMD_FILE_DELETE = "FILE_DELETE";
	/** File request for download */
	public final static String CMD_FILE_REQUEST = "FILE_REQUEST";
	/** File receive request for upload */
	public final static String CMD_FILE_RECEIVE_REQUEST = "FILE_RECEIVE_REQUEST";
	
	// Server-side
	//------------------------------------------------------------------------------
	
	/**
	 * Write/send client answer from server to client.
	 * 
	 * @param client answer
	 * @param out output stream
	 * @throws Excpetion
	 */
	public static void writeAnswer(ClientAnswer answer, DataOutputStream out) throws IOException {
		
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
	public static ServerCommand readCommand(DataInputStream in) throws IOException {
	    return ServerCommand.parse(read(in));
	}
	
	
	// Helper functions
	//------------------------------------------------------------------------------
	
	/**
	 * Server- or client-side read.
	 * 
	 * @param in input stream
	 * @return unparsed data
	 * @throws IOException
	 */
	protected static String read(DataInputStream in) throws IOException {
		
		final int length = in.readInt();
		
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
	
	/**
	 * Safe close for closeable object  (e.g. stream, socket).
	 * 
	 * @param closeable closeable object
	 */
    public static final void safeClose(Object closeable) {
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
