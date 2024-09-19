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
package ch.autumo.beetroot;
 
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.nanohttpd.protocols.http.HTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.NanoHTTPD.ResponseException;
import org.nanohttpd.protocols.http.content.ContentType;
import org.nanohttpd.protocols.http.content.CookieHandler;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.protocols.http.tempfiles.ITempFileManager;
import org.nanohttpd.router.RouterNanoHTTPD.UriResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.handler.ErrorHandler;

/**
 * BeetRoot HTTP session.
 */
public class BeetRootHTTPSession extends HTTPSession {

	protected static final Logger LOG = LoggerFactory.getLogger(BeetRootHTTPSession.class.getName());
	
	static {
		
		int kBytes = BeetRootConfigurationManager.getInstance().getInt("ws_response_buffer_size");
		if (kBytes == -1) {
			LOG.warn("Using 16 kBytes for response buffer size.");
			kBytes = 16;
		}
		
		RESPONSE_BUFFER_SIZE = kBytes * 1024;
		System.setProperty("ch.autumo.beetroot.respDownBufSizeKB", "" + kBytes);
		
		int kBytesDown = BeetRootConfigurationManager.getInstance().getInt("ws_response_download_buffer_size");
		if (kBytesDown == -1) {
			LOG.warn("Using 8 kBytes for response download buffer size.");
			kBytesDown = 8;
		}
		
		RESPONSE_DOWNLOAD_BUFFER_SIZE = kBytesDown * 1024;
	}
	
	// buffer size
	public static final long RESPONSE_BUFFER_SIZE;

	// download buffer size
	public static final long RESPONSE_DOWNLOAD_BUFFER_SIZE;
	
    private String externalSessionId;

    
	public BeetRootHTTPSession(NanoHTTPD httpd, ITempFileManager tempFileManager, InputStream inputStream, OutputStream outputStream) {
		super(httpd, tempFileManager, inputStream, outputStream);
	}

    public BeetRootHTTPSession(NanoHTTPD httpd, ITempFileManager tempFileManager, InputStream inputStream, OutputStream outputStream, InetAddress inetAddress) {
    	super(httpd, tempFileManager, inputStream, outputStream, inetAddress);
    }

    
    /**
     * Constructor used for external servlet containers.
     * 
     * @param externalSessionId external session id
     * @param tempFileManager temp file manager
     * @param inputStream http input stream
     */
    public BeetRootHTTPSession(String externalSessionId, ITempFileManager tempFileManager, InputStream inputStream) {
    	
    	this(null, tempFileManager, inputStream, null);
    	this.externalSessionId = externalSessionId;
    }
    
    public String getExternalSessionId() {
		return externalSessionId;
	}
    
    /**
     * Provide a method for executing the request and response for the HTTP body only
     * from the servlet context in this nano/beetRoot session.
     * 
     * @param beetRootService beetRoot service, in this case the beetRoor server when running in a web container!
     * @param request HTTP request
     * @param response HTTP response
     * @throws IOException IO exception
     */
    public void executeForServlet(BeetRootService beetRootService, HttpServletRequest request, HttpServletResponse response) throws IOException {
    	
        final BeetRootWebServer externalBeetrootServer = (BeetRootWebServer) beetRootService;
        Response r = null;
    	
    	try {
    	
            super.parms = new HashMap<String, List<String>>();
            if (null == this.headers) {
                this.headers = new HashMap<String, String>();
            } else {
                this.headers.clear();
            }
            
            
            // Decode the header into parms and header java properties
            Map<String, String> pre = new HashMap<String, String>();
            
            decodeHeaderForServlet(request, pre, this.parms, this.headers);

            super.method = Method.lookup(pre.get("method"));
            if (super.method == null) {
                throw new ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Syntax error. HTTP verb " + pre.get("method") + " unhandled.");
            }

            super.uri = pre.get("uri");
            super.cookies = new CookieHandler(this.headers);

            final String connection = this.headers.get("connection");
            final boolean keepAlive = "HTTP/1.1".equals(protocolVersion) && (connection == null || !connection.matches("(?i).*close.*"));

            
            r = externalBeetrootServer.serve(this, request);


            if (r == null) {
            	
                throw new ResponseException(Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
                
            } else {
				
                r.setKeepAlive(keepAlive);

            	// is it a download?
            	final String downHeaderVal = r.getHeader("Content-disposition");
            	if (downHeaderVal != null) {
            		response.setContentType(r.getMimeType());
            		response.setHeader("Content-disposition", downHeaderVal);
            		final byte[] buffer = new byte[Long.valueOf(RESPONSE_BUFFER_SIZE).intValue()];
            		int numBytesRead;
                    while ((numBytesRead = r.getData().read(buffer)) > 0) {
                        response.getOutputStream().write(buffer, 0, numBytesRead);
                    }
                    
                    // manually necessary here, because sendBody isn't called
                    r.close();
                    
                // Everything else
            	} else {
            		r.sendBody(response.getOutputStream(), -1);
            	}
            }
            
            if (!keepAlive || r.isCloseConnection()) {
                throw new SocketException("BeetRootWebServer (within Servlet) connection closed!");
            }	            
        
        } catch (ResponseException re) {
        	
            LOG.error("Response exception occured!", re);

            final Session userSession = this.getUserSession();
            
            final String t = LanguageManager.getInstance().translate("base.err.srv.re.title", userSession);
            final String m = LanguageManager.getInstance().translate("base.err.srv.re.msg", userSession, re.getStatus().getRequestStatus(), re.getMessage());
            final UriResource uriResource = new UriResource(null, 100, ErrorHandler.class, Status.INTERNAL_ERROR, t, m);
            final Map<String, String> urlParams = new HashMap<String, String>();
            final Response errorResponse = uriResource.process(urlParams, this);
            errorResponse.sendBody(response.getOutputStream(), -1);
            
            //response.getWriter().write(re.getMessage());
            
        } finally {
        	
            super.tempFileManager.clear();
        }
    }
    
    /**
     * Provide a method for decoding the header from the servlet context in this nano session.
     * 
     * @param request http request
     * @param pre map to fill
     * @param parmsURI  parameters to fill
     * @param headers headers to fill
     * @throws ResponseException response exception
     */
    private void decodeHeaderForServlet(HttpServletRequest request, Map<String, String> pre, Map<String, List<String>> parms, Map<String, String> headers) throws ResponseException {

        pre.put("method", request.getMethod());
        
		final Enumeration<String> paramKeys = request.getParameterNames();
        while (paramKeys.hasMoreElements()) {
			final String paramKey = paramKeys.nextElement();
			parms.put(paramKey, Arrays.asList(request.getParameterValues(paramKey)));
		}

        protocolVersion = request.getProtocol();
        if (protocolVersion == null || protocolVersion.length() == 0)
            protocolVersion = "HTTP/1.1";

        
        final Enumeration<String> headerKeys = request.getHeaderNames();
        while (headerKeys.hasMoreElements()) {
			final String headerKey = headerKeys.nextElement();
			final String headerVal = request.getHeader(headerKey).trim();
			headers.put(headerKey.trim().toLowerCase(Locale.US), headerVal);
		}
        
        pre.put("uri", request.getRequestURI());
    }
    
    /**
     * Provide a method for parsing the body from a servlet context
     * in this nano session.
     * 
     * @param files the map for the files
     * @param request HttpServletRequest 
     * @throws IOException IO exception
     * @throws ResponseException response exception
     */
    public void parseBodyForServlet(Map<String, String> files, HttpServletRequest request) throws IOException, ResponseException {
    	
    	// application/x-www-form-urlencoded
    	//     or
    	// multipart/form-data; boundary=----WebKitFormBoundaryB3QUUDmQwvkxJRid
    	
    	final String cType = request.getContentType().toLowerCase();
    	// NOTICE: we don't do anything with posted data yet except it is a multipart!
    	if (!cType.startsWith(ContentType.MULTIPART_FORM_DATA_HEADER))
    		return;

		Collection<Part> parts;
		try {
			parts = request.getParts();
		} catch (Exception e) {
			LOG.error("Can't retrieve file upload part(s) from request!", e);
			throw new ResponseException(Status.INTERNAL_ERROR, "Can't retrieve file upload part(s) from request!", e);
		}
    	
		LOOP: for (Part part : parts) {
			
			final String partName = part.getName();
			if (partName.startsWith("MAX") || partName.startsWith("_"))
				continue LOOP;
			
			int size = (int) part.getSize();
			InputStream inputStream = part.getInputStream();
			byte[] bytes = new byte[inputStream.available()];
			inputStream.read(bytes);
			
            ByteBuffer fbuf = ByteBuffer.wrap(bytes, 0, bytes.length);
           	final String path = saveTmpFile(fbuf, 0, size, part.getSubmittedFileName());
            files.put(partName, path);
            
			List<String> values = super.parms.get(partName);
            if (values == null) {
                values = new ArrayList<String>();
                super.parms.put(partName, values);
            }
            values.add(part.getSubmittedFileName());
		}
    }  
    
    /**
     * Get user session for this HTTP session.
     * 
     * @return user session
     */
    public Session getUserSession() {
    	return SessionManager.getInstance().findOrCreate(this);
    }
    
    /**
     * Update one parameter. It is only updated, if a matching value
     * for this key exists and it's a single value, not a list of 
     * parameter values. 
     *  
     * @param key key
     * @param value new value
     */
    public void updateParameter(String key, String value) {
    	List<String> values = this.parms.get(key);
    	// we only update if there's one value for this key!
    	if (values != null && values.size() == 1) {
    		values = new ArrayList<String>();
    		values.add(value);
    		this.parms.put(key, values);
    	}
    }

    /**
     * Overwrites existing parameter or adds it if it doens't
     * exist.
     * 
     * @param key key
     * @param value new value
     */
    public void overwriteParameter(String key, String value) {
    	List<String> values = this.parms.get(key);
    	// we only update if there's one value for this key!
    	if (values != null && values.size() == 1) {
    		this.updateParameter(key, value);
    	} else {
    		values = new ArrayList<String>();
    		values.add(value);
    		this.parms.put(key, values);
    	}
    }
    
}
