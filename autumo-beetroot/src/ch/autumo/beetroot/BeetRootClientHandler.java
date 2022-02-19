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
                LOG.error("Communication with the client broken, or an bug in the handler code", e);
            }
        } finally {
        	NanoHTTPD.safeClose(this.nanoAcceptSocket);
        	NanoHTTPD.safeClose(this.nanoInputStream);
        	NanoHTTPD.safeClose(this.nanoAcceptSocket);
        	
        	nanoHttpd.getAsyncRunner().closed(this);
        }
    }
    
}
