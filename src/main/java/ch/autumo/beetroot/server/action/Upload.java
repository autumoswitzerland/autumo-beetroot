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

import ch.autumo.beetroot.server.message.file.UploadRequest;

/**
 * Upload.
 */
public class Upload extends FileAction {

	/**
	 * Divider character for entity holding file name and check-sum.
	 */
	public static String ENTITY_DIVIDER_FILENAME_CHECKSUM = ""+UploadRequest.ENTITY_DIVIDER_FILENAME_CHECKSUM;
	
	private long size = -1;
	private String user = null;
	private String checkSum = null;
	
	/**
	 * Upload.
	 * 
	 * @param size file size
	 * @param fileName file name
	 * @param checkSum file check.sum
	 * @param user user or null
	 * @param domain domain or null (default)
	 */
	public Upload(long size, String fileName, String checkSum, String user, String domain) {
		super(fileName, domain);
		this.checkSum = checkSum;
		this.size = size;
		this.user = user;
	}
	
	public long getSize() {
		return size;
	}

	public String getUser() {
		return user;
	}

	public String getCheckSum() {
		return checkSum;
	}
	
	@Override
	public boolean equals(Object obj) {
		return size == ((Upload)obj).size && checkSum.equals(((Upload)obj).checkSum);
	}
	
}
