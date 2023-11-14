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
