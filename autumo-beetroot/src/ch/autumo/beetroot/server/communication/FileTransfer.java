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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.server.action.Download;
import ch.autumo.beetroot.utils.Utils;

/**
 * Client/Server file transfer.
 */
public class FileTransfer {

	protected final static Logger LOG = LoggerFactory.getLogger(FileTransfer.class.getName());
	
	/** file server default port */
	public static int DEFAULT_FILE_SERVER_PORT = 9777;
	/** file receiver default port */
	public static int DEFAULT_FILE_RECEIVER_PORT = 9779;
	
	/** File get */
	public final static String CMD_FILE_GET = "FILE_GET";
	
	/** default buffer length in Kb for sending bits of a file */
	public static int DEFAULT_BUFFER_LEN = 32;
	
	/** buffer length for sending bits of a file */
	protected static int bufferLen = 1024 * DEFAULT_BUFFER_LEN;
	
	static {
		// Buffer size
		bufferLen = BeetRootConfigurationManager.getInstance().getInt(Constants.KEY_ADMIN_FILE_BUF_SIZE);
		if (bufferLen == -1)
			bufferLen = DEFAULT_BUFFER_LEN;
		
		bufferLen = bufferLen * 1024;
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
	public static void writeFile(Download download, DataOutputStream output) throws IOException {
		
		final FileInputStream fileInputStream = new FileInputStream(download.getFile());
		
        // send file size
		output.writeLong(download.getFile().length());  
        // break file into chunks
        final byte buffer[] = new byte[bufferLen];
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
	
	
	// Server and Client-side
	//------------------------------------------------------------------------------
	
	/**
	 * Server/client side file read.
	 * 
	 * @param in input stream
	 * @param fileName file name
	 * @param size file size previously read!
	 * @return server temporary file or null, if file received was invalid
	 * @throws IOException
	 */
	public static File readFile(DataInputStream in, String fileName, long size) throws IOException {

		long length = size;
		final File f = new File(Utils.getTemporaryDirectory() + fileName);
		final FileOutputStream fileOutputStream = new FileOutputStream(f);
		final byte buffer[] = new byte[bufferLen];
		int bytes = 0;
        while (length > 0 && (bytes = in.read(buffer, 0, (int)Math.min(buffer.length, length))) != -1) {
            fileOutputStream.write(buffer,0,bytes);
            length -= bytes; // read up to file size
        }
        fileOutputStream.close();
		
	    return f;
	}	
	
}
