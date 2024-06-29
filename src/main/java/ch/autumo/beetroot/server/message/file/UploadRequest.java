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
package ch.autumo.beetroot.server.message.file;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

import ch.autumo.beetroot.server.communication.Communicator;
import ch.autumo.beetroot.server.message.ServerCommand;

/**
 * Upload request; client-side.
 */
public class UploadRequest extends ServerCommand {

	private File file = null;
	
	/**
	 * Divider character for entity holding file name and check-sum.
	 */
	public static char ENTITY_DIVIDER_FILENAME_CHECKSUM = '/';
	
	/**
	 * File command to send before sending a file to
	 * server.
	 * 
	 * @param file file
	 * @param user user or null
	 * @param domain domain or null (default)
	 * @throws IOException IO exception
	 */
	public UploadRequest(File file, String user, String domain) throws IOException {
		
		super(DISPATCHER_ID_INTERNAL, Communicator.CMD_FILE_RECEIVE_REQUEST, null, file.length(), domain);
		
		// Set user if any
		if (user != null)
			super.setObject(user); // set the user into the general transfer object

		this.setFile(file);
	}
	
	/**
	 * Set file.
	 * 
	 * @param file file
	 * @throws IOException IO exception
	 */
	public void setFile(File file) throws IOException {
		
		this.file = file;
		
		// Set Entity which is a combination of file-name and checksum
		final String absPath = file.getAbsolutePath();
		final Path path = Paths.get(absPath); 
        final String fileName = path.getFileName().toString(); 

	    byte data[];
	    String checkSum = null;
		try {
			data = Files.readAllBytes(path);
		    byte hash[] = MessageDigest.getInstance("MD5").digest(data);
		    checkSum = new BigInteger(1, hash).toString(16);
		} catch (Exception e) {
			throw new IOException("Couldn't build checksum for file '" + absPath + "!", e);
		}
        
		super.setEntity(fileName + ENTITY_DIVIDER_FILENAME_CHECKSUM + checkSum);		
	}

	/**
	 * Get file.
	 *  
	 * @return file
	 */
	public File getFile() {
		return this.file;
	}
	
}
