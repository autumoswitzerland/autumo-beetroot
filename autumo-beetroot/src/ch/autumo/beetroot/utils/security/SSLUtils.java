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
package ch.autumo.beetroot.utils.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.security.SecureApplicationHolder;


/**
 * SSL Utils.
 */
public class SSLUtils {

	/**
	 * Get key-store file.
	 * 
	 * @return key-store file
	 */
	public static String getKeystoreFile() {
		String keystoreFile = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_KEYSTORE_FILE);
		if (!keystoreFile.startsWith("/"))
			keystoreFile = "/" + keystoreFile;		
		return keystoreFile;
	}
	
	/**
	 * Get key-store password.
	 * 
	 * @return key-store password
	 * @throws Exception
	 */
	public static char[] getKeystorePw() throws Exception {
		return SSLUtils.getKeystorePw(BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_PW_ENC));
	}

	/**
	 * Get key-store password.
	 * 
	 * @param encodedPassword is the password encoded in configuration?
	 * @return key-store password
	 * @throws Exception
	 */
	public static char[] getKeystorePw(boolean encodedPassword) throws Exception {
		final String keystorepw = encodedPassword ? 
				BeetRootConfigurationManager.getInstance().getDecodedString(Constants.KEY_KEYSTORE_PW, SecureApplicationHolder.getInstance().getSecApp()) : 
					BeetRootConfigurationManager.getInstance().getString(Constants.KEY_KEYSTORE_PW);
		return keystorepw.toCharArray();
	}
	
    /**
     * Creates an SSLServerSocketFactory. Pass a KeyStore resource with your
     * certificate and pass-phrase.
	 * 
	 * @param keyAndTrustStoreClasspathPath key-store file class-path reference
	 * @param passphrase pass phrase
	 * @return SSL server socket factory
	 * @throws IOException
	 */
    public static SSLServerSocketFactory makeSSLServerSocketFactory(String keyAndTrustStoreClasspathPath, char passphrase[]) throws IOException {
        try {
            final KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            final InputStream keystoreStream = SSLUtils.class.getResourceAsStream(keyAndTrustStoreClasspathPath);
            if (keystoreStream == null) {
                throw new IOException("Unable to load keystore from classpath: " + keyAndTrustStoreClasspathPath);
            }
            keystore.load(keystoreStream, passphrase);
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, passphrase);
        	final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);
            final SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            return ctx.getServerSocketFactory();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }


    /**
     * Creates an SSLSocketFactory. Pass a KeyStore resource with your
     * certificate and pass-phrase.
	 * 
	 * @param keyAndTrustStoreClasspathPath key-store file class-path reference
	 * @param passphrase pass phrase
	 * @return SSL socket factory
	 * @throws IOException
	 */
    public static SSLSocketFactory makeSSLSocketFactory(String keyAndTrustStoreClasspathPath, char passphrase[]) throws IOException {
        try {
            final KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            final InputStream keystoreStream = SSLUtils.class.getResourceAsStream(keyAndTrustStoreClasspathPath);
            if (keystoreStream == null) {
                throw new IOException("Unable to load keystore from classpath: " + keyAndTrustStoreClasspathPath);
            }
            keystore.load(keystoreStream, passphrase);
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, passphrase);
        	final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);
            final SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            return  ctx.getSocketFactory();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
    
}
