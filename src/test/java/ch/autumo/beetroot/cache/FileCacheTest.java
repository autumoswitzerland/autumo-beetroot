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
package ch.autumo.beetroot.cache;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootDatabaseManager;

/**
 * File cache test.
 */
public class FileCacheTest {

	private static List<Path> allFiles = null;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		BeetRootConfigurationManager.getInstance().initialize("cfg/beetroot_test.cfg");
		BeetRootDatabaseManager.getInstance().initialize();

		allFiles = new ArrayList<>(); 
		
		Path p1 = Paths.get("src/main/java"); 
		listAllFiles(p1, allFiles); 
		Path p2 = Paths.get("web/"); 
		listAllFiles(p2, allFiles); 
		Path p3 = Paths.get("doc/"); 
		listAllFiles(p3, allFiles); 
	}

	@Before
	public void setUp() throws Exception {
	}


	@Test
	public void testSize() throws IOException {
		boolean hasSpace = false;
		
		//Path lpath = null;
		LOOP: for (Iterator<Path> iterator = allFiles.iterator(); iterator.hasNext();) {
			Path path = iterator.next();
			long fs = path.toFile().length();
			hasSpace =FileCacheManager.getInstance().hasSpace(0, fs);
			if (hasSpace) {
				FileCache fc = FileCacheManager.getInstance().findOrCreate(path);
				System.out.println("FC: Adding "+fc.getFullPath());
			} else {
				assertFalse("Still has space!", FileCacheManager.getInstance().hasSpace(0, fs));
				System.out.println("Size: "+ FileCacheManager.getInstance().getSize());
				System.out.println("=====================");
				//lpath = path;
				break LOOP;
			}
		}
		/** 
		long size = FileCacheManager.getInstance().getSize();
		System.out.println("FC: Size = "+size);
		System.out.println("Force cache last file... : " + lpath.getFileName());
		FileCacheManager.getInstance().findOrCreate(lpath, true);
		long size2 = FileCacheManager.getInstance().getSize();
		System.out.println("FC: Size = "+FileCacheManager.getInstance().getSize());
		*/
		
		FileCacheManager.getInstance().clear();
		System.out.println("FC: Size = "+FileCacheManager.getInstance().getSize());
		assertTrue("File cahce should be zero!", FileCacheManager.getInstance().getSize() == 0);
	}

	@Test
	public void testBinary() throws IOException {
		
		Path p1 = Paths.get("lib/repo");
		listAllFiles(p1, allFiles, "jar");
		allFiles.forEach(System.out::println);
		File f = allFiles.get(0).toFile();
		FileCache fc = FileCacheManager.getInstance().findOrCreate(f.toPath());
		System.out.println("FC: Adding binary file "+fc.getFullPath());
		assertTrue("File should not be cached!", fc.isCached() == false);
	}
	
	
	@After
	public void tearDown() throws Exception {
		FileCacheManager.getInstance().clear();
		allFiles.clear();
	}

	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	
	private static void listAllFiles(Path currentPath, List<Path> allFiles, String... extensions) throws IOException { 
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath))  { 
			for (Path entry : stream) { 
				if (Files.isDirectory(entry)) { 
					listAllFiles(entry, allFiles, extensions); 
				} else {
					if (extensions == null || extensions.length == 0) {
						allFiles.add(entry); 
					} else {
						LOOP: for (int i = 0; i < extensions.length; i++) {
							if (entry.toFile().getName().endsWith(extensions[i])) {
								allFiles.add(entry); 
								break LOOP;
							}
						}
					}
				} 
			} 
		} 
	}
   
}
