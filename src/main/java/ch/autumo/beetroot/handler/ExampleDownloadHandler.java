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

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.utils.Utils;

/**
 * Default file download handler for 'web/html/files/view.html' templates.
 */
public class ExampleDownloadHandler extends BaseHandler {

	protected final static Logger LOG = LoggerFactory.getLogger(ExampleDownloadHandler.class.getName());
	
	public ExampleDownloadHandler(String entity) {
		super(entity);
	}

	public ExampleDownloadHandler(String entity, String msg) {
		super(entity);
		this.addSuccessMessage(msg);
	}

	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		
		final String requestedFilename = session.getParms().get("download");

		// It doesn't really matter what's the file name here.
		// Decide which file you want to deliver based 
		// on requested file name.
		// Set valid file and mime type and type to response!
		
		if (requestedFilename == null) {
			
			// Just show page!

			return null;
			
		} else {
			
			// Download !
			
			final HandlerResponse downloadResponse = new HandlerResponse(HandlerResponse.STATE_OK);
			downloadResponse.setType(HandlerResponse.TYPE_FILE_DOWNLOAD);
			downloadResponse.setDownloadFileMimeType(Constants.MIME_TYPES_MAP.getContentType(requestedFilename));
			
			File f = null;
			final ServletContext context = BeetRootConfigurationManager.getInstance().getServletContext();
			if (context != null) {
				// NOTE: This example will not work in jetty; we would 
				// need to load the resource from the war archive 
				f = new File(Utils.getRealPath(context) + requestedFilename);
			}
			else
				f = new File(BeetRootConfigurationManager.getInstance().getRootPath() + requestedFilename);

			downloadResponse.setDownloadFile(f);
			
			return downloadResponse;
		}
	}
	
	@Override
	public String getResource() {
		return "web/html/:lang/"+entity+"/view.html";
	}
	
}
