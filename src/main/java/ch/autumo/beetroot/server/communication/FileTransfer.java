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
import ch.autumo.beetroot.utils.systen.OS;

/**
 * Client/Server file transfer.
 */
public class FileTransfer {

	protected static final Logger LOG = LoggerFactory.getLogger(FileTransfer.class.getName());
	
	/** file server default port */
	public static int DEFAULT_FILE_SERVER_PORT = 9777;
	/** file receiver default port */
	public static int DEFAULT_FILE_RECEIVER_PORT = 9779;
	
	/** File get */
	public static final String CMD_FILE_GET = "FILE_GET";
	
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
	 * @param download download file
	 * @param output output stream
	 * @throws IOException IO exception
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
	 * @throws IOException IO exception
	 */
	public static File readFile(DataInputStream in, String fileName, long size) throws IOException {

		long length = size;
		final File f = new File(OS.getTemporaryDirectory() + fileName);
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
