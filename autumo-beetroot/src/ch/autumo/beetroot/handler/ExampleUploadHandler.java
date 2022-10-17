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

	protected final static Logger LOG = LoggerFactory.getLogger(ExampleUploadHandler.class.getName());
	
	public ExampleUploadHandler(String entity) {
		super(entity);
	}

	public ExampleUploadHandler(String entity, String errMsg) {
		super(entity);
		this.addErrorMessage(errMsg);
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
		return HomeHandler.class;
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
