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
	 * @throws Exception
	 */
	public String store(File file, String name, String user, String domain) throws Exception;

	/**
	 * Find a file (latest version). The file delivered within the download must be
	 * physically temporarily available, so it can be delivered by
	 * a stream.
	 *   
	 * @param uniqueFileId unique file id
	 * @param domain domain or null (default)
	 * @return download or null if file is not available
	 * @throws Exception
	 */
	public Download findFile(String uniqueFileId, String domain) throws Exception;
	
	/**
	 * Delete a file.
	 * 
	 * @param uniqueFileId unique file id
	 * @param domain domain or null (default)
     * @return true if at least one (of all versions) has been found and deleted
	 * @throws Exception
	 */
	public boolean delete(String uniqueFileId, String domain) throws Exception;
	
}
