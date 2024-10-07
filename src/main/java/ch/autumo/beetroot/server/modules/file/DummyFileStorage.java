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
package ch.autumo.beetroot.server.modules.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.server.action.Download;
import ch.autumo.beetroot.server.modules.FileStorage;
import ch.autumo.beetroot.utils.systen.OS;


/**
 * Dummy file storage - just an example of a simple file storage;
 * not to be used for serious and productive applications!
 */
public class DummyFileStorage implements FileStorage {

	protected static final Logger LOG = LoggerFactory.getLogger(DummyFileStorage.class.getName());
	
	private static String storageLocation;
	static {
		final String tempDir = OS.getTemporaryDirectory();
		storageLocation = BeetRootConfigurationManager.getInstance().getString("dummy_file_storage_location", tempDir);
		final File dir = new File(storageLocation);
		if (!dir.exists() || !dir.isDirectory()) {
			LOG.warn("Dummy file storage location '{}' is invalid, using temporary directory '{}'.", storageLocation, tempDir);
			storageLocation = tempDir;
		}
		LOG.info("Dummy file storage location: '{}'.", storageLocation);
	}
	
	@Override
	public String store(File file, String name, String user, String domain) throws Exception {
		final String dir = this.findOrCreateDirectory(storageLocation, domain);
        final String validName = sanitizeFileName(name);
        final Path filePath = Paths.get(dir, validName);
        try {
            Files.copy(file.toPath(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return validName;
        } catch (IOException e) {
			LOG.error("Failed to store file!", e);
            throw new Exception("Failed to store file", e);
        }
	}

	@Override
	public Download findFile(String uniqueFileId, String domain) throws Exception {
		final String dir = this.findOrCreateDirectory(storageLocation, domain);
		final Path dirPath = Paths.get(dir);
		Optional<Path> found;
        try (Stream<Path> stream = Files.list(dirPath)) {
            found = stream
                    .filter(path -> path.getFileName().toString().equals(uniqueFileId))
                    .findFirst(); // Return the first match
        }		
		if (!found.isEmpty() ) {
			final Path file = found.get();
			final Path tempFile = Files.createTempFile("temp_", "_copy.txt");
            // Copy the original file to the temporary file
            Files.copy(file, tempFile, StandardCopyOption.REPLACE_EXISTING);
			// It is always necessary to reference a temporary file for the download,
            // because the server will ALWAYS delete the referenced file in the download
            // after sending it to the client
			return new Download(uniqueFileId, uniqueFileId, tempFile.toFile(), domain);
		}
		LOG.warn("File '{}' not found!", dir + uniqueFileId);
		return null;
	}

	@Override
	public boolean delete(String uniqueFileId, String domain) throws Exception {
		final String dir = this.findOrCreateDirectory(storageLocation, domain);
		final File f = new File(dir + uniqueFileId);
		if (f.exists()) {
			return f.delete();
		}
		LOG.warn("File '{}' not found and not deleted!", dir + uniqueFileId);
		return false;
	}

	private String findOrCreateDirectory(String location, String domain) {
		String loc = location;
		if (!loc.endsWith(OS.FILE_SEPARATOR))
			loc += OS.FILE_SEPARATOR;
		if (domain != null)
			loc += domain.replace(" ", "_").trim();
		if (!loc.endsWith(OS.FILE_SEPARATOR))
			loc += OS.FILE_SEPARATOR;
		final File dirs = new File(loc);
		dirs.mkdirs();
		return loc;
	}
	
    private String sanitizeFileName(String name) {
        // Replace any invalid characters (for example, replace spaces or special characters)
        return name.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
    }
    
}
