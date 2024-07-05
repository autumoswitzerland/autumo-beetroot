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

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;

/**
 * File cache manager.
 */
public class FileCacheManager {

	protected final static Logger LOG = LoggerFactory.getLogger(FileCacheManager.class.getName());

	// file buffer size
	static {
		int mBytes = BeetRootConfigurationManager.getInstance().getInt("ws_cache_size");
		
		if (mBytes == -1) {
			LOG.warn("Using 2 MBytes for max. cache size.");
			mBytes = 2;
		}
		
		MAX_CACHE_SIZE = mBytes * 1024 * 1024;
	}
	public static final long MAX_CACHE_SIZE; 
	
	private static FileCacheManager instance = null;	
	
	private Map<String, FileCache> cacheMap = new ConcurrentHashMap<String, FileCache>();
	
	private long size = 0;
	
	
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
	 * Is there space left in the cache?
	 * 
	 * @param oldValueInBytes old amount of bytes that have been used
	 * 			for the same resource
	 * @param newValueInBytes new amount of bytes that is used by the
	 * 			changed resource
	 * @return true, is there is space, otherwise false
	 */
	public synchronized boolean hasSpace(long oldValueInBytes, long newValueInBytes) {

		long csize = this.size; 
		csize -= oldValueInBytes;
		csize += newValueInBytes;
		
		if (csize > MAX_CACHE_SIZE)
			return false;
		else
			return true;
	}
	
	/**
	 * Update cache size.
	 * 
	 * @param oldValueInBytes old amount of bytes that have been used
	 * 			for the same resource
	 * @param newValueInBytes new amount of bytes that is used by the
	 * 			changed resource
	 * @return new cache size;
	 */
	public synchronized long updateCacheSize(long oldValueInBytes, long newValueInBytes) {
		size -= oldValueInBytes;
		size += newValueInBytes;
		return size;
	}

	/**
	 * Find or create file cache.
	 * 
	 * @param url URL file path
	 * @return file cache
	 * @throws IOException IO exception
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
	 * 
	 * @param uri URI file path
	 * @return file cache
	 * @throws IOException IO exception
	 */
	public FileCache findOrCreate(URI uri) throws IOException {
		
		Path p = Paths.get(uri);
		return findOrCreate(p.toAbsolutePath());
	}
	
	/**
	 * Find or create file cache.
	 * 
	 * @param path file path
	 * @return file cache
	 * @throws IOException IO exception
	 */
	public FileCache findOrCreate(String path) throws IOException {
		
		Path p = Paths.get(path);
		return findOrCreate(p.toAbsolutePath());
	}

	/**
	 * Find or create file cache.
	 * 
	 * @param path file path
	 * @param forcedCaching caching is forced when true if max. cache size isn't reached; 
	 * 			force caching breaks the file size limit, but not the cache size limit!
	 * @return file cache
	 * @throws IOException IO exception
	 */
	public FileCache findOrCreate(String path, boolean forceCaching) throws IOException {
		
		Path p = Paths.get(path);
		return findOrCreate(p.toAbsolutePath(), forceCaching);
	}
	
	/**
	 * Find or create file cache.
	 * 
	 * @param path file path
	 * @return file cache
	 * @throws IOException IO exception
	 */
	public FileCache findOrCreate(Path path) throws IOException {
		
		final String pstr = path.toAbsolutePath().toString();
		final String mimeType = Constants.MIME_TYPES_MAP.getContentType(pstr);
		if (!cacheMap.containsKey(pstr)) {
		
			final FileCache fc = new FileCache(path.toAbsolutePath(), mimeType);
			cacheMap.put(pstr, fc);
			
			LOG.trace("FileCache added: " + fc.getFullPath() + ", cachesize="+this.size);
			
			return fc;
		}
		return cacheMap.get(pstr);
	}

	/**
	 * Find or create file cache.
	 * 
	 * @param path resource path
	 * @param forcedCaching caching is forced when true if max. cache size isn't reached; 
	 * 			force caching breaks the file size limit, but not the cache size limit!
	 * @return file cache
	 * @throws IOException IO exception
	 */
	public FileCache findOrCreate(Path path, boolean forcedCaching) throws IOException {
		
		final String pstr = path.toAbsolutePath().toString();
		final String mimeType = Constants.MIME_TYPES_MAP.getContentType(pstr);
		if (!cacheMap.containsKey(pstr)) {
		
			final FileCache fc = new FileCache(path.toAbsolutePath(), mimeType, forcedCaching);
			cacheMap.put(pstr, fc);
			
			LOG.trace("FileCache added: " + fc.getFullPath() + ", cachesize="+this.size);
			
			return fc;
		}
		return cacheMap.get(pstr);
	}
	
	/**
	 * Find or create file cache.
	 * 
	 * @param resourcePath resource path
	 * @return file cache
	 * @throws IOException IO exception
	 */
	public FileCache findOrCreateByResource(String resourcePath) throws IOException {
		
		final String rstr = "resource:" + resourcePath; 
		final String mimeType = Constants.MIME_TYPES_MAP.getContentType(resourcePath);
		if (!cacheMap.containsKey(rstr)) {
		
			final FileCache fc = new FileCache(resourcePath, mimeType);
			
			LOG.trace("FileCache added: " + fc.getFullPath() + ", cachesize="+this.size);
			
			cacheMap.put(rstr, fc);
			return fc;
		}
		return cacheMap.get(rstr);
	}

	/**
	 * Is the file already in cache?
	 * 
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
	 * @param resourcePath resource path
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
		this.size = 0;
		cacheMap.clear();
	}
	
	/**
	 * Get maximum size of this cache.
	 * @return max. size
	 */
	public long getMaxSize() {
		return MAX_CACHE_SIZE;
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
