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

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.Utils;

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
