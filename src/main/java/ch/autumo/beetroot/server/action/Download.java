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
package ch.autumo.beetroot.server.action;

import java.io.File;

/**
 * Download.
 */
public class Download extends FileAction {

	private String fileId = null;
	private File file = null;

	/**
	 * Download.
	 * 
	 * @param fileId unique file ID
	 * @param fileName file name
	 * @param file temporary file to download
	 * @param domain domain
	 */
	public Download(String fileId, String fileName, File file, String domain) {
		super(fileName, domain);
		this.file = file;
		this.fileId = fileId;
	}

	public String getFileId() {
		return fileId;
	}

	public File getFile() {
		return file;
	}

	@Override
	public boolean equals(Object obj) {
		return fileId.equals(((Download)obj).fileId);
	}
	
}
