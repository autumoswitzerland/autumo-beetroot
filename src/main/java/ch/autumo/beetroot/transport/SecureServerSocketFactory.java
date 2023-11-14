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
import java.net.ServerSocket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * Secure Server Socket Factory.
 */
public class SecureServerSocketFactory implements ServerSocketFactory {

    private SSLServerSocketFactory sslServerSocketFactory;
    private String[] sslProtocols;

    public SecureServerSocketFactory(SSLServerSocketFactory sslServerSocketFactory, String[] sslProtocols) {
        this.sslServerSocketFactory = sslServerSocketFactory;
        this.sslProtocols = sslProtocols;
    }

    @Override
    public ServerSocket create(int port) throws IOException {
        SSLServerSocket ss = null;
        ss = (SSLServerSocket) this.sslServerSocketFactory.createServerSocket(port);
        if (this.sslProtocols != null) {
            ss.setEnabledProtocols(this.sslProtocols);
        } else {
            ss.setEnabledProtocols(ss.getSupportedProtocols());
        }
        ss.setUseClientMode(false);
        ss.setWantClientAuth(false);
        ss.setNeedClientAuth(false);
        return ss;
    }
    
	/**
	 * Return the base (java.net) SSL server socket factory.
	 * 
	 * @return base (java.net) SSL server socket factory
	 */
	public SSLServerSocketFactory getBaseServerSocketFactory() {
		return this.sslServerSocketFactory;
	}
    
}
