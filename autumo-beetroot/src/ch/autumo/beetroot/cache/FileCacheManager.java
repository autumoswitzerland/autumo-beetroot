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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.ConfigurationManager;
import ch.autumo.beetroot.Constants;

/**
 * File cache manager.
 */
public class FileCacheManager {

	protected final static Logger LOG = LoggerFactory.getLogger(FileCacheManager.class.getName());

	// file buffer size
	static {
		int mBytes = ConfigurationManager.getInstance().getInt("ws_cache_size");
		
		if (mBytes == -1) {
			LOG.warn("Using 2 MBytes for max. cache size.");
			mBytes = 2;
		}
		
		MAX_CACHE_SIZE = mBytes * 1024;
	}
	public static final long MAX_CACHE_SIZE; 
	
	private static FileCacheManager instance = null;	
	
	private Map<String, FileCache> cacheMap = new ConcurrentHashMap<String, FileCache>();
	
	private long size = 0;
	private boolean maxSizeReached = false;
	
	
	/**
	 * File cache manager.
	 * 
	 * @return file manager
	 */
	public static FileCacheManager getInstance() {
        if (instance == null)
        	instance = new FileCacheManager();
 
        return instance;
    }

	/**
	 * Get size of cache.
	 * @return cache size
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Is max. cache size reached?
	 * @return true, if so
	 */
	public boolean isMaxSizeReached() {
		return maxSizeReached;
	}

	/**
	 * Is there space left in the cache?
	 * 
	 * @param oldValueInBytes old amount of bytes that have been used
	 * 			for the sam resource
	 * @param newValueInBytes new amount of bytes that is used by the
	 * 			changed resource
	 * @return true, is there is space, otherwise false
	 */
	public boolean hasSpace(long oldValueInBytes, long newValueInBytes) {

		long csize = this.size; 
		csize -= oldValueInBytes;
		csize += oldValueInBytes;
		
		if (csize > MAX_CACHE_SIZE)
			return false;
		else
			return true;
	}
	
	/**
	 * Update cache size.
	 * 
	 * @param oldValueInBytes old amount of bytes that have been used
	 * 			for the sam resource
	 * @param newValueInBytes new amount of bytes that is used by the
	 * 			changed resource
	 * @return true if max. cache size has been reached, otherwise false
	 */
	public boolean updateCacheSize(long oldValueInBytes, long newValueInBytes) {
		
		size -= oldValueInBytes;
		size += oldValueInBytes;
		
		if (size > MAX_CACHE_SIZE)
			maxSizeReached = true;
		else
			maxSizeReached = false;
		
		return maxSizeReached;
	}

	/**
	 * Find or create file cache.
	 * @param url URL file path
	 * @return file cache
	 * @throws IOException
	 */
	public FileCache findOrCreate(URL url) throws IOException {
		
		Path p;
		try {
			p = Paths.get(url.toURI());
		} catch (URISyntaxException e) {
			throw new IOException("Couldn't parse URI!", e);
		}
		
		return findOrCreate(p.toAbsolutePath());
	}
	
	/**
	 * Find or create file cache.
	 * @param uri URI file path
	 * @return file cache
	 * @throws IOException
	 */
	public FileCache findOrCreate(URI uri) throws IOException {
		
		Path p = Paths.get(uri);
		return findOrCreate(p.toAbsolutePath());
	}
	
	/**
	 * Find or create file cache.
	 * @param path file path
	 * @return file cache
	 * @throws IOException
	 */
	public FileCache findOrCreate(String path) throws IOException {
		
		Path p = Paths.get(path);
		return findOrCreate(p.toAbsolutePath());
	}

	/**
	 * Find or create file cache.
	 * @param path file path
	 * @param forcedCaching caching is forced when true if max. cache size isn't reached.
	 * @return file cache
	 * @throws IOException
	 */
	public FileCache findOrCreate(String path, boolean forceCaching) throws IOException {
		
		Path p = Paths.get(path);
		return findOrCreate(p.toAbsolutePath(), forceCaching);
	}
	
	/**
	 * Find or create file cache.
	 * @param path file path
	 * @param forcedCaching caching is forced when true if max. cache size isn't reached.
	 * @return file cache
	 * @throws IOException
	 */
	public FileCache findOrCreate(Path path) throws IOException {
		
		final String pstr = path.toAbsolutePath().toString();
		final String mimeType = Constants.MIME_TYPES_MAP.getContentType(pstr);
		if (!cacheMap.containsKey(pstr)) {
		
			final FileCache fc = new FileCache(path.toAbsolutePath(), mimeType);
			cacheMap.put(pstr, fc);
			return fc;
		}
		return cacheMap.get(pstr);
	}

	/**
	 * Find or create file cache.
	 * @param resourcePath resource path
	 * @param forcedCaching caching is forced when true if max. cache size isn't reached.
	 * @return file cache
	 * @throws IOException
	 */
	public FileCache findOrCreate(Path path, boolean forcedCaching) throws IOException {
		
		final String pstr = path.toAbsolutePath().toString();
		final String mimeType = Constants.MIME_TYPES_MAP.getContentType(pstr);
		if (!cacheMap.containsKey(pstr)) {
		
			final FileCache fc = new FileCache(path.toAbsolutePath(), mimeType, forcedCaching);
			cacheMap.put(pstr, fc);
			return fc;
		}
		return cacheMap.get(pstr);
	}
	
	/**
	 * Find or create file cache.
	 * @param resourcePath resource path
	 * @return file cache
	 * @throws IOException
	 */
	public FileCache findOrCreateByResource(String resourcePath) throws IOException {
		
		final String rstr = "resource:" + resourcePath; 
		final String mimeType = Constants.MIME_TYPES_MAP.getContentType(resourcePath);
		if (!cacheMap.containsKey(rstr)) {
		
			final FileCache fc = new FileCache(resourcePath, mimeType);
			cacheMap.put(rstr, fc);
			return fc;
		}
		return cacheMap.get(rstr);
	}

	/**
	 * Is the file already in cache?
	 * @param path file path
	 * @return true if so
	 */
	public boolean contains(String path) {
		Path p = Paths.get(path);
		final String pstr = p.toAbsolutePath().toString();
		return cacheMap.containsKey(pstr);
	}

	/**
	 * Is the resource (e.g. JAR resource) already in cache?
	 * @param resourcePath resouce path
	 * @return true if so
	 */
	public boolean containsResource(String resourcePath) {
		final String rstr = "resource:" + resourcePath; 
		return cacheMap.containsKey(rstr);
	}
	
	/**
	 * Clear cache.
	 */
	public void clear() {
		final Set<String> set = cacheMap.keySet();
		for (Iterator<String> iterator = set.iterator(); iterator.hasNext();) {
			final String key = (String) iterator.next();
			final FileCache fCache = cacheMap.get(key);
			fCache.clear();
		}
		cacheMap.clear();
	}
	
	
	/**
	 * Get normalized path.
	 * @param path path
	 * @return normalized path
	 */
	public static String getNormalizedPath(String path) {
		Path p = Paths.get(path);
		return p.toAbsolutePath().toString();
	}
	
}
