package ch.autumo.beetroot.utils;

import java.util.List;

import ch.autumo.beetroot.BeetRootConfigurationManager;

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
