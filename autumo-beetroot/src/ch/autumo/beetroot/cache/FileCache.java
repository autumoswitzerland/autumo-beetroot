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
package ch.autumo.beetroot.cache;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.nanohttpd.protocols.http.content.ContentType;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.ConfigurationManager;
import ch.autumo.beetroot.Utils;

/**
 * File cache.
 */
public class FileCache  {

	protected final static Logger LOG = LoggerFactory.getLogger(FileCache.class.getName());
	
	// file buffer size
	static {
		int kBytes = ConfigurationManager.getInstance().getInt("ws_file_cache_size");
		
		if (kBytes == -1) {
			LOG.warn("Using 100 kBytes for file cache size.");
			kBytes = 100;
		}
		
		BUFFER_LIMIT = kBytes * 1024;
	}
	public static final long BUFFER_LIMIT; 
	
	private File file = null;
	private Path filePath = null;
	private long fileSize = 0;
	private long cacheSize = 0;
	private ContentType contentType = null;
	private String mimeType = null;
	
	private byte buffer[] = null;		// this OR 
	private char textBuffer[] = null;	// this
	
	private long lastModified = -1;
	
	private boolean isCached = false;
	private boolean forcedCaching = false;
	
	private boolean isArchive = false;
	private boolean isBinary = false;
	private boolean isText = false;
	
	private boolean isResource = false;
	
	
	/**
	 * File cache constructor.
	 * 
	 * @param filePath file path
	 * @param mimeType mime type, e.g. "text/html"
	 * @throws IOException
	 */
	public FileCache(Path filePath, String mimeType) throws IOException {
		
		this(filePath, getContentType(mimeType));
		this.mimeType = mimeType;
	}
	
	/**
	 * File cache constructor.
	 * 
	 * @param filePath file path
	 * @param contentType content header type, e.g. 
	 * 			"text/html; charset=UTF-8"
	 * @throws IOException
	 */
	public FileCache(Path filePath, ContentType contentType) throws IOException {
		
		this.filePath = filePath;
		this.contentType = contentType;

		this.mimeType = contentType.getContentType();
		
		this.isArchive = Utils.isMimeTypeArchive(mimeType); 
		this.isBinary = Utils.isMimeTypeOctet(mimeType); 
		this.isText = Utils.isMimeTypeText(mimeType); 
		
		this.file = filePath.toFile();
		
		if (!this.file.exists()) {
			throw new FileNotFoundException("File not found: " + this.file.getPath());
		}
		
		this.fileSize = file.length();
		this.lastModified = file.lastModified();

		if (!this.isArchive && fileSize <= BUFFER_LIMIT && FileCacheManager.getInstance().hasSpace(0, this.fileSize)) {

			final FileInputStream fis = new FileInputStream(file);
			if (isBinary)
				buffer = IOUtils.toByteArray(fis, fileSize);
			if (isText)
				textBuffer = IOUtils.toCharArray(fis, contentType.getEncoding());
			
			fis.close();
			
			FileCacheManager.getInstance().updateCacheSize(0, this.fileSize);			
			this.cacheSize = this.fileSize;
			this.isCached = true;
		}
	}
	
	/**
	 * File cache constructor.
	 * 
	 * @param filePath file path
	 * @param mimeType mime type, e.g. "text/html"
	 * @param forcedCaching force caching?
	 * @throws IOException
	 */
	public FileCache(Path filePath, String mimeType, boolean forcedCaching) throws IOException {
		
		this(filePath, getContentType(mimeType), forcedCaching);
		this.mimeType = mimeType;
	}
	
	/**
	 * File cache constructor.
	 * 
	 * @param filePath file path
	 * @param contentType content header type, e.g. 
	 * 			"text/html; charset=UTF-8"
	 * @param forcedCaching force caching?
	 * @throws IOException
	 */
	public FileCache(Path filePath, ContentType contentType, boolean forcedCaching) throws IOException {
		
		this.filePath = filePath;
		this.contentType = contentType;

		this.mimeType = contentType.getContentType();
		
		this.isArchive = Utils.isMimeTypeArchive(mimeType); 
		this.isBinary = Utils.isMimeTypeOctet(mimeType); 
		this.isText = Utils.isMimeTypeText(mimeType); 
		
		this.forcedCaching = forcedCaching;
		
		this.file = filePath.toFile();
		
		if (!this.file.exists()) {
			throw new FileNotFoundException("File not found: " + this.file.getPath());
		}
		
		this.fileSize = file.length();
		this.lastModified = file.lastModified();

		if (!this.isArchive && (this.forcedCaching || this.fileSize <= BUFFER_LIMIT) && FileCacheManager.getInstance().hasSpace(0, this.fileSize)) {
		
			final FileInputStream fis = new FileInputStream(file);
			if (isBinary)
				buffer = IOUtils.toByteArray(fis, fileSize);
			if (isText)
				textBuffer = IOUtils.toCharArray(fis, contentType.getEncoding());
			fis.close();
			
			FileCacheManager.getInstance().updateCacheSize(0, this.fileSize);			
			this.cacheSize = this.fileSize;
			this.isCached = true;
		}
	}
	
	/**
	 * File cache constructor.
	 * 
	 * @param resourcePath resource path
	 * @throws IOException
	 */
	public FileCache(String resourcePath) throws IOException {
		
		this(resourcePath, (String) null);
	}
	
	/**
	 * File cache constructor.
	 * 
	 * @param resourcePath resource path
	 * @param mimeType mime type, e.g. "text/html"
	 * @throws IOException
	 */
	public FileCache(String resourcePath, String mimeType) throws IOException {
		
		this(resourcePath, getContentType(mimeType));
	}	
	
	/**
	 * File cache constructor.
	 * 
	 * @param resourcePath resource path
	 * @param contentType content header type, e.g. 
	 * 			"text/html; charset=UTF-8"
	 * @throws IOException
	 */
	public FileCache(String resourcePath, ContentType contentType) throws IOException {
		this.resourceInitialization(resourcePath, contentType);
	}

	private void resourceInitialization(String resourcePath, ContentType contentType) throws IOException {
		
		this.contentType = contentType;
		this.mimeType = contentType.getContentType();
		
		this.isArchive = Utils.isMimeTypeArchive(mimeType); 
		this.isBinary = Utils.isMimeTypeOctet(mimeType); 
		this.isText = Utils.isMimeTypeText(mimeType); 
		
		this.isResource = true;
		
		// we swallow the bitter pill for text- and octet-based files within JAR's
		// and always cache atm. We also ignore size! 
		// they cannot be changed anyway! We also cannot read the length of the file
		// beforehand.
		if (!isArchive) {
			final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
			if (is == null)
				throw new IOException("Resource '" + resourcePath + "' cannot be read.");
				
			if (isBinary) {
				buffer = IOUtils.toByteArray(is);
				this.cacheSize = buffer.length;
			}
			if (isText) {
				textBuffer = IOUtils.toCharArray(is, contentType.getEncoding());
				this.cacheSize = textBuffer.length;
			}
			is.close();
			
			FileCacheManager.getInstance().updateCacheSize(0, this.cacheSize);			
			this.fileSize = this.cacheSize;
			this.isCached = true;
		}
		
		this.lastModified = 1;		
	}
	
	/**
	 * Get data as stream from file or cache, depending of the buffer size.
	 * 
	 * @return data stream
	 * @throws IOException
	 */
    public InputStream getData() throws IOException {
    	
    	final long newLastModified = isResource ? -1 : file.lastModified();
    	
    	if (newLastModified > lastModified) {
    		
    		final long oldCacheSize = this.cacheSize;
    		this.fileSize = this.file.length();
    		this.lastModified = newLastModified;
    		
    		if (!this.isArchive && (forcedCaching || fileSize <= BUFFER_LIMIT) && FileCacheManager.getInstance().hasSpace(oldCacheSize, this.fileSize)) {
    		
    			final FileInputStream fis = new FileInputStream(file);
    			this.buffer = IOUtils.toByteArray(fis, fileSize);
    			this.cacheSize = buffer.length;    			
    			fis.close();
    			FileCacheManager.getInstance().updateCacheSize(oldCacheSize, this.cacheSize);			
    			this.isCached = true;
    			LOG.trace("FileCache re-cached: " + this.getFullPath() + ", cachesize="+FileCacheManager.getInstance().getSize());
    			
    		} else {
    			FileCacheManager.getInstance().updateCacheSize(oldCacheSize, 0);			
    			this.cacheSize = 0;
    			this.isCached = false;
    		}
    	} 
    	
		if (isCached)
	    	return new ByteArrayInputStream(buffer);
		else
			return new FileInputStream(file);
    }	

    /**
     * Get text data.
     * 
     * @return cached data as text or null
	 * @throws IOException
     */
    public String getTextData()  throws IOException{
    	
    	if (isBinary || isArchive)
    		throw new IOException("FileCache: getTextData() can only be called on text file caches!");
    	
		if (!isCached)
    		throw new IOException("FileCache: getTextData() caleld on a non-cached file, first check if it's cached!");

    	
    	final long newLastModified = isResource ? -1 : file.lastModified();
    	
    	if (newLastModified > lastModified) {
    		
    		final long oldCacheSize = this.cacheSize;
    		this.fileSize = this.file.length();
    		this.lastModified = newLastModified;
    		
    		if (!this.isArchive && (forcedCaching || fileSize <= BUFFER_LIMIT) && FileCacheManager.getInstance().hasSpace(oldCacheSize, this.fileSize)) {
    		
    			final FileInputStream fis = new FileInputStream(file);
				this.textBuffer = IOUtils.toCharArray(fis, contentType.getEncoding());
    			this.cacheSize = textBuffer.length;
    			FileCacheManager.getInstance().updateCacheSize(oldCacheSize, this.cacheSize);			
    			fis.close();
    			this.isCached = true;
    			LOG.trace("FileCache re-cached: " + this.getFullPath() + ", cachesize="+FileCacheManager.getInstance().getSize());
    		} else {
    			FileCacheManager.getInstance().updateCacheSize(oldCacheSize, 0);			
    			this.cacheSize = 0;
    			this.isCached = false;
    		}
    	} 
    	
    	return new String(textBuffer);
    }
    
    /**
     * Create response out of cached data.
     * @param mimeType mime type
     * 
     * @return response
     */
    public Response createResponse(String mimeType) throws IOException {

    	if (isText) {
    		
    		if (isCached)
    			return Response.newFixedLengthResponse(Status.OK, mimeType, getTextData());
    		else
    			return Response.newFixedLengthResponse(Status.OK, mimeType, this.getData(), -1);
    		
    	} else {
    		
        	if (isResource)
        		return Response.newFixedLengthResponse(Status.OK, mimeType, this.getData(), -1);
        	else
            	return Response.newFixedLengthResponse(Status.OK, mimeType, this.getData(), this.fileSize);
        	
    	}
    }
    
    /**
     * Create response out of cached data.
     * 
     * @return response
     */
    public Response createResponse() throws IOException {
    	
    	return this.createResponse(this.contentType.getContentType());
    }
    
    /**
     * Return mime type if any
     * @return mime type
     */
    public String getMimeType() {
    	if (this.mimeType != null)
    		return this.mimeType.trim();
    	else
    		return null;
    }

    /**
     * Return encoding.
     * 
     * @return encoding
     */
    public String getEncoding() {
    	if (this.contentType == null)
    		return "UTF-8";
    	else
    		return this.contentType.getEncoding();
    }
    
    /**
     * Get file full pat.
     *  
     * @return file path
     */
    public String getFullPath() {
    	
    	return this.filePath.toAbsolutePath().toString();
    }
    
	private static ContentType getContentType(String mimeType) {
		
		final ContentType ct;
		if (mimeType == null)
			ct = new ContentType("text/ANY; charset=UTF-8");
		else if (mimeType.equalsIgnoreCase("text/html"))
			ct = new ContentType("text/html; charset=UTF-8");
		else if (mimeType.equalsIgnoreCase("text/plain"))
			ct = new ContentType("text/plain; charset=UTF-8");
		else
			ct = new ContentType(mimeType.trim() + "; charset=UTF-8");
		
		return ct;
	}

	/**
	 * Cached? 
	 * @return true if so
	 */
	public boolean isCached() {
		return isCached;
	}

	/**
	 * When was the file last modified or '-1' if it is a resource.
	 * @return last modification of file or -1
	 */
    public long getLastModified() {
		return lastModified;
	}

    /**
     * Is it an archive?
     * @return is it an archive
     */
	public boolean isArchive() {
		return isArchive;
	}

    /**
     * Is it a binary?
     * @return is it a binary
     */
	public boolean isBinary() {
		return isBinary;
	}

    /**
     * Is it a text file?
     * @return is it a text file
     */
	public boolean isText() {
		return isText;
	}

	/**
	 * Is it a resource (e.g. from JAR)?
	 * @return is it a resoruce
	 */
	public boolean isResource() {
		return isResource;
	}
	
	/**
	 * Clear cache if cached.
	 */
	public void clear() {
		if (textBuffer != null)
			textBuffer = null;
		if (buffer != null)
			buffer = null;
	}

	@Override
	public boolean equals(Object obj) {
		return getFullPath().equals(((FileCache) obj).getFullPath());
	}
	
}
