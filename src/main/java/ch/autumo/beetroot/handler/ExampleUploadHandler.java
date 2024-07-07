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
package ch.autumo.beetroot.handler;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.LanguageManager;
import ch.autumo.beetroot.Session;

/**
 * Default file upload handler for 'web/html/files/add.html' templates.
 */
public class ExampleUploadHandler extends BaseHandler {

	protected static final Logger LOG = LoggerFactory.getLogger(ExampleUploadHandler.class.getName());
	
	public ExampleUploadHandler(String entity) {
		super(entity);
	}

	public ExampleUploadHandler(String entity, String errMsg) {
		super(entity);
		this.addSuccessMessage(errMsg);
	}

	@Override
	public HandlerResponse saveData(BeetRootHTTPSession session) throws Exception {
		
		final Session userSession = session.getUserSession();

		// Original file name
		String origFileName = session.getParms().get("file");
		
		LOG.info("Original name of uploaded file: "+origFileName);
		
		final Map<String, String> files = userSession.consumeFiles();
		Set<String> keys = files.keySet();
		
		for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
			
			final String key = iterator.next();
			LOG.info("Parameter key for uploaded file: "+key);
			
			final String path = files.get(key);
			LOG.info("Temporary file path of file: "+path);

			if (new File(path).exists())
				LOG.info("...and the file exists for further processing!");
		}

		final HandlerResponse hs = new HandlerResponse(HandlerResponse.STATE_OK, LanguageManager.getInstance().translate("base.info.stored1", userSession, origFileName));
		hs.setType(HandlerResponse.TYPE_FILE_UPLOAD);
		return hs;
	}
	
	@Override
	public String getResource() {
		return "web/html/:lang/"+entity+"/add.html";
	}

	@Override
	public Class<?> getRedirectHandler() {
		return ExampleUploadHandler.class;
	}

	@Override
	public String replaceTemplateVariables(String line, BeetRootHTTPSession session) {

		int mus = BeetRootConfigurationManager.getInstance().getInt("web_max_upload_size");
		if (mus == -1) {
			mus = 32;
			LOG.warn("Using 32 MB for max. upload file size.");
		}
		
		if (line.contains("{$maxFileUploadSizeMb}")) {
			
			line = line.replace("{$maxFileUploadSizeMb}", ""+mus);
		}
		if (line.contains("{$maxFileUploadSize}")) {
			
			line = line.replace("{$maxFileUploadSize}", "" + (mus * 1024 * 1024));
		}
		
		return line;
	}
	
}
