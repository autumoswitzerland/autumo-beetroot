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
package ch.autumo.beetroot.transport;

import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Secure Socket Factory.
 */
public class SecureSocketFactory implements SocketFactory {

    private SSLSocketFactory sslSocketFactory;
    private String[] sslProtocols;
	
    public SecureSocketFactory(SSLSocketFactory sslSocketFactory, String[] sslProtocols) {
        this.sslSocketFactory = sslSocketFactory;
        this.sslProtocols = sslProtocols;
    }
	
	@Override
	public Socket create(String host, int port) throws IOException {
        SSLSocket ss = null;
        ss = (SSLSocket) this.sslSocketFactory.createSocket(host, port);
        if (this.sslProtocols != null) {
            ss.setEnabledProtocols(this.sslProtocols);
        } else {
            ss.setEnabledProtocols(ss.getSupportedProtocols());
        }
        return ss;
	}
	
	/**
	 * Return the base (java.net) SSL socket factory.
	 * 
	 * @return base (java.net) SSL socket factory
	 */
	public SSLSocketFactory getBaseSocketFactory() {
		return this.sslSocketFactory;
	}

}
