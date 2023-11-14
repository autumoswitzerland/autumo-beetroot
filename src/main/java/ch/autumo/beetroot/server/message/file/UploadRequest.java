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

import ch.autumo.beetroot.server.communication.Communicator;
import ch.autumo.beetroot.server.message.ServerCommand;

/**
 * Upload request; client-side.
 */
public class UploadRequest extends ServerCommand {

	/**
	 * File command to send before sending a file to
	 * server.
	 * 
	 * @param fileName file name
	 * @param user user or null
	 * @param domain domain or null (default)
	 * @param size file size
	 */
	public UploadRequest(String fileName, String user, String domain, long size) {
		super(DISPATCHER_ID_INTERNAL, Communicator.CMD_FILE_RECEIVE_REQUEST, fileName, size, domain);
		if (user != null)
			super.setObject(user); // set the user into the general transfer object
	}
	
}
