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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.nanohttpd.protocols.http.ClientHandler;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.tempfiles.ITempFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BeetRoot client handler,
 */
public class BeetRootClientHandler extends ClientHandler {

	protected final static Logger LOG = LoggerFactory.getLogger(BeetRootClientHandler.class.getName());
	
    private final BeetRootWebServer nanoHttpd;
    private final InputStream nanoInputStream;
    private final Socket nanoAcceptSocket;
	
    public BeetRootClientHandler(BeetRootWebServer httpd, InputStream inputStream, Socket acceptSocket) {
    	
    	super(httpd, inputStream, acceptSocket);
    	
    	this.nanoHttpd= httpd;
    	this.nanoInputStream = inputStream;
    	this.nanoAcceptSocket = acceptSocket;
    }

    @Override
    public void run() {
        OutputStream outputStream = null;
        try {
            outputStream = this.nanoAcceptSocket.getOutputStream();
            ITempFileManager tempFileManager = nanoHttpd.getTempFileManagerFactory().create();
            
            // This accesses javax-servlet-api!
            IHTTPSession session = new BeetRootHTTPSession(nanoHttpd, tempFileManager, this.nanoInputStream, outputStream, this.nanoAcceptSocket.getInetAddress());
            
            while (!this.nanoAcceptSocket.isClosed()) {
                session.execute();
            }
        } catch (Exception e) {
            // When the socket is closed by the client,
            // we throw our own SocketException
            // to break the "keep alive" loop above. If
            // the exception was anything other
            // than the expected SocketException OR a
            // SocketTimeoutException, print the
            // stacktrace
            if (!(e instanceof SocketException && "NanoHttpd Shutdown".equals(e.getMessage())) && !(e instanceof SocketTimeoutException)) {
                LOG.error("Communication with the client broken, or a bug in the handler code", e);
            }
        } finally {
            NanoHTTPD.safeClose(outputStream);
        	NanoHTTPD.safeClose(this.nanoInputStream);
        	NanoHTTPD.safeClose(this.nanoAcceptSocket);
        	
        	nanoHttpd.getAsyncRunner().closed(this);
        }
    }
    
}
