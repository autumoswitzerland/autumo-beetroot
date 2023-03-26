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
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.server.message.ClientAnswer;
import ch.autumo.beetroot.server.message.ServerCommand;

/**
 * Client/Server communication.
 */
public class Communicator {

	protected final static Logger LOG = LoggerFactory.getLogger(Communicator.class.getName());

	/** Max. message size: 64 kBytes */
	public final static int MAX_MSG_SIZE = 64 * 1024;
	
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
	
	/**
	 * User agents used for tunneled server commands. 
	 */
	public static final String USER_AGENT = "beetRoot-Client";
	
	/**
	 * HTTP Header accept with 'application/json'.
	 */
	public static final String[] HTTP_HEADER_ACCEPT_JSON = new String[] {"Accept", "application/json"};
	
	/**
	 * HTTP Header accept with 'application/json; charset=utf-8'.
	 */
	public static final String[] HTTP_HEADER_CONTENTTYPE_JSON_UTF8 = new String[] {"Content-Type", "application/json; charset=UTF-8"};
	
	
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

	/**
	 * Read a JSON server command server side.
	 * 
	 * @param in (body) input stream from HTTP/HTTPS request
	 * @param length length of content
	 * @return server command or null, if command received was invalid
	 * @throws IOException
	 */
	public static ServerCommand readJsonCommand(InputStream in, int length) throws IOException {
		
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
	    
	    return ServerCommand.parseJson(dataString.toString());
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
		
		// In any case, if the length read is too big, e.g., when a malformed client request is sent,
		// this could lead to a out-of-memory-error in the heap space!
		// --> Taking care of this: In any case the max. message size must be 64Kb,
		//     considering also it contains an additional serialized object.
		if (length > MAX_MSG_SIZE) {
			// We could do many things here, but in fact it is an invalid message,
			// therefore raise an exception!
			throw new IOException("The communication message received is bigger than '"+MAX_MSG_SIZE+"' bytes, that's an invalid message!");
		}
		
		final byte[] messageByte = new byte[length];
	    boolean end = false;
	    final StringBuilder dataString = new StringBuilder(length); //TODO !
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
    
	/**
	 * Check closeable HTTP response with HttpResponse.
	 * 
	 * @param resp HTTP response
	 * @throws Exception
	 */
	public static void checkHttpResponse(CloseableHttpResponse resp) throws Exception {
		
		final int code = resp.getStatusLine().getStatusCode();
		final String reason = resp.getStatusLine().getReasonPhrase();
		
		if (204 != code && 200 != code) {
			
			if (reason != null && reason.length() > 0) {
				throw new Exception(reason + " / HTTP Status: " + code);
			} else {
				throw new Exception(resp.toString() + " / HTTP Status: " + code);
			}
		}
	}
	
	/**
	 * Is it an internal command?
	 * 
	 * @param command server command
	 * @return true if so
	 */
	protected static boolean isInternalCommand(ServerCommand command) {
		return command.getCommand().equals(Communicator.CMD_HEALTH)
				|| command.getCommand().equals(Communicator.CMD_STOP);
	}
	
}
