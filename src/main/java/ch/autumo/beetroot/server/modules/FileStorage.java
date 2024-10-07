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
package ch.autumo.beetroot.server.modules;

import java.io.File;

import ch.autumo.beetroot.server.action.Download;

/**
 * File storage interface.
 */
public interface FileStorage {
	
	/**
	 * Store a file.
	 * 
	 * @param file file
	 * @param name file name
	 * @param user user or null
	 * @param domain domain or null (default)
	 * @return unique file ID
	 * @throws Exception exception
	 */
	public String store(File file, String name, String user, String domain) throws Exception;

	/**
	 * Find a file (latest version). The file delivered within the download must be
	 * physically temporarily available, so it can be delivered by a stream and the
	 * file referenced by the download will ALWAYS be deleted after sending it to the
	 * client; so it is necessary to create a temporary file in the download -
	 * never reference the original file, e.g. when a real file structure is used
	 * as a file storage!
	 *   
	 * @param uniqueFileId unique file id
	 * @param domain domain or null (default)
	 * @return download or null if file is not available
	 * @throws Exception exception
	 */
	public Download findFile(String uniqueFileId, String domain) throws Exception;
	
	/**
	 * Delete a file.
	 * 
	 * @param uniqueFileId unique file id
	 * @param domain domain or null (default)
     * @return true if at least one (of all versions) has been found and deleted
	 * @throws Exception exception
	 */
	public boolean delete(String uniqueFileId, String domain) throws Exception;
	
}
