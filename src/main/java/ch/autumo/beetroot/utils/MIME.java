/**
 * 
 * Copyright (c) 2024 autumo Ltd. Switzerland, Michael Gasche
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
package ch.autumo.beetroot.utils;

import java.util.List;

import ch.autumo.beetroot.BeetRootConfigurationManager;

/**
 * MIME checks.
 */
public class MIME {

	/** Allowed text mime types. */
	public static List<String> mimeTextList;
	/** Allowed octet mime types. */
	public static List<String> mimeOctetList;
	/** Allowed archive mime types. */
	public static List<String> mimeArchiveList;
	
	/**
	 * Is supported text mime type?
	 * 
	 * @param mimeType mime type
	 * @return true if so
	 */
	public static boolean isMimeTypeText(String mimeType) {
		
		if (mimeTextList == null)
			mimeTextList = BeetRootConfigurationManager.getInstance().getMimeTypes("ws_mime_allowed_text");
		return mimeTextList.contains(mimeType);
	}

	/**
	 * Is supported octet-stream mime type?
	 * 
	 * @param mimeType mime type
	 * @return true if so
	 */
	public static boolean isMimeTypeOctet(String mimeType) {
		if (mimeOctetList == null)
			mimeOctetList = BeetRootConfigurationManager.getInstance().getMimeTypes("ws_mime_allowed_octet");
		return mimeOctetList.contains(mimeType);
	}
	
	/**
	 * Is supported archive mime type?
	 * 
	 * @param mimeType mime type
	 * @return true if so
	 */
	public static boolean isMimeTypeArchive(String mimeType) {
		if (mimeArchiveList == null)
			mimeArchiveList = BeetRootConfigurationManager.getInstance().getMimeTypes("ws_mime_allowed_archive");
		return mimeArchiveList.contains(mimeType);
	}
	
}
