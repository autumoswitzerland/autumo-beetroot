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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.utils.Utils;

/**
 * Client/Server file transfer.
 */
public class FileTransfer {

	protected final static Logger LOG = LoggerFactory.getLogger(FileTransfer.class.getName());
	
	/** File get */
	public final static String CMD_FILE_GET = "FILE_GET";

	/** file server default port */
	public static int DEFAULT_PORT = 9777;
	/** file server default port */
	public static int DEFAULT_RECEIVER_PORT = 9779;
	
	/** default buffer length for sending bits of a file */
	public static int DEFAULT_BUFFER_LEN = 32;
	
	/** buffer length for sending bits of a file */
	private static int bufferLenKb = DEFAULT_BUFFER_LEN;
	
	/** file server port */
	private static int portFileServer = -1;
	/** file receiver port (file-store end-point) */
	private static int portFileReceiver = -1;
	/** file server host */
	private static String hostAdmin = null;
	
	/** client timeout iof configured */
	private static int clientTimeout = -1;
	
	/** use SSL sockets? */
	private static boolean sslSockets = false;
	
	
	static {
		
		// read some undocumented settings if available
		clientTimeout = BeetRootConfigurationManager.getInstance().getIntNoWarn("client_timeout"); // in ms !
		
		// SSL sockets?
		final String mode = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_COM_ENC);
		sslSockets = (mode != null && mode.equalsIgnoreCase("ssl"));
		
		// File server port
		portFileServer = BeetRootConfigurationManager.getInstance().getInt(Constants.KEY_ADMIN_FILE_PORT);
		if (portFileServer == -1) {
			LOG.error("File server port not specified! Using port '" + DEFAULT_PORT + "'.");
			portFileServer = DEFAULT_PORT;
		}

		// File server port
		portFileReceiver = BeetRootConfigurationManager.getInstance().getInt(Constants.KEY_ADMIN_FILE_RECEIVER_PORT);
		if (portFileReceiver == -1) {
			//LOG.error("File receiver port not specified! Using port '" + DEFAULT_RECEIVER_PORT + "'.");
			portFileReceiver = DEFAULT_RECEIVER_PORT;
		}
		
		// File server host
		hostAdmin = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_HOST);
		// Buffer size
		
		bufferLenKb = BeetRootConfigurationManager.getInstance().getInt(Constants.KEY_ADMIN_FILE_BUF_SIZE);
		if (bufferLenKb == -1)
			bufferLenKb = DEFAULT_BUFFER_LEN;
	}		
	
	
	// Client-side
	//------------------------------------------------------------------------------
	
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
				
			if (sslSockets) {
				final SocketFactory sslsocketfactory = SSLSocketFactory.getDefault();
				socket = sslsocketfactory.createSocket(hostAdmin, portFileReceiver);				
			} else {
				socket = new Socket(hostAdmin, portFileReceiver);
			}
			
			final FileInputStream fileInputStream = new FileInputStream(file);
			
			output = new DataOutputStream(socket.getOutputStream());
			socket.setSoTimeout(timeout);

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
			return FileTransfer.readAnswer(input);
			
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
	
	
	// Server-side
	//------------------------------------------------------------------------------
	
	/**
	 * Write/send file from server to client.
	 * 
	 * @param Downlaod download file
	 * @param out output stream
	 * @throws Excpetion
	 */
	protected static void writeFile(Download download, DataOutputStream output) throws IOException {
		
		final FileInputStream fileInputStream = new FileInputStream(download.getFile());
		
        // send file size
		output.writeLong(download.getFile().length());  
        // break file into chunks
        final byte buffer[] = new byte[bufferLenKb];
        int bytes = 0;
        while ((bytes = fileInputStream.read(buffer)) != -1) {
        	output.write(buffer, 0, bytes);
        	output.flush();
        }
        fileInputStream.close();

		LOG.trace("Server file '" + download.getFileName() + "' sent!");
        
		// At last we delete the temporary file, it is consumed now !
        if (download.getFile().exists())
        	download.getFile().delete();
	}
	
	/**
	 * Read a server file server side.
	 * 
	 * @param in input stream
	 * @param fileId unique file id
	 * @return server temporary file or null, if file received was invalid
	 * @throws IOException
	 */
	protected static File readFile(DataInputStream in, String fileId) throws IOException {
	    return read(in, fileId);
	}
	
	/**
	 * Server side file read.
	 * 
	 * @param in input stream
	 * @return server temporary file or null, if file received was invalid
	 * @throws IOException
	 */
	private static File read(DataInputStream in, String fileName) throws IOException {

		final File f = new File(Utils.getTemporaryDirectory() + fileName);
		final FileOutputStream fileOutputStream = new FileOutputStream(f);
		long length = in.readLong();
		final byte buffer[] = new byte[bufferLenKb];
		int bytes = 0;
        while (length > 0 && (bytes = in.read(buffer, 0, (int)Math.min(buffer.length, length))) != -1) {
            fileOutputStream.write(buffer,0,bytes);
            length -= bytes;      // read upto file size
        }
        fileOutputStream.close();
		
	    return f;
	}	
	
}
