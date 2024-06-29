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
package ch.autumo.beetroot.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.security.SecureApplicationHolder;


/**
 * SSL utilities.
 */
public class SSL {

	/**
	 * HTTP/HTTPS socket factory registry.
	 * 
	 * Used for http/https tunneling of server command on the client side.
	 * We create it once if needed!
	 */
	private static Registry<ConnectionSocketFactory> httpSocketFactoryRegistry = null;
	
	/**
	 * SSL client connection factory.
	 * 
	 * Used for http/https tunneling of server command on the client side.
	 * We create it once if needed!
	 */
	private static SSLConnectionSocketFactory httpSslSf = null;
	
	/**
	 * Host-name verification when an SSL/HTTPS certificate is used?
	 * Usually with self-signed certificates and on localhost this is
	 * turned off, because the verification doesn't work.
	 */
	private static final boolean verifyHost;
	static {
		verifyHost = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_COM_HOSTNAME_VERIFY);
	} 
	
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
	 * @throws Exception exception
	 */
	public static char[] getKeystorePw() throws Exception {
		return SSL.getKeystorePw(BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_PW_ENC));
	}

	/**
	 * Get key-store password.
	 * 
	 * @param encodedPassword is the password encoded in configuration?
	 * @return key-store password
	 * @throws Exception exception
	 */
	public static char[] getKeystorePw(boolean encodedPassword) throws Exception {
		final String keystorepw = encodedPassword ? 
				BeetRootConfigurationManager.getInstance().getDecodedString(Constants.KEY_KEYSTORE_PW, SecureApplicationHolder.getInstance().getSecApp()) : 
					BeetRootConfigurationManager.getInstance().getString(Constants.KEY_KEYSTORE_PW);
		return keystorepw.toCharArray();
	}

    /**
     * Creates an SSLContext. Pass a KeyStore resource with your
     * certificate and pass-phrase. Parameters read from configuration. 
	 * 
	 * @return SSL context
	 * @throws Exception exception
	 */
    public static SSLContext makeSSLContext() throws Exception {
    	return makeSSLContext(getKeystoreFile(), getKeystorePw());
    }
	
    /**
     * Creates an SSLContext. Pass a KeyStore resource with your
     * certificate and pass-phrase.
	 * 
	 * @param keyAndTrustStore key-store file class-path reference or full path
	 * @param passphrase pass phrase
	 * @return SSL context
	 * @throws IOException IO exception
	 */
    public static SSLContext makeSSLContext(String keyAndTrustStore, char passphrase[]) throws IOException {
        try {
            final KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream keystoreStream = SSL.class.getResourceAsStream(keyAndTrustStore);
            
            if (keystoreStream == null) {
            	if (keyAndTrustStore != null) {
            		// get as file
            		File f = new File(keyAndTrustStore);
            		if (f.exists()) {
            			keystoreStream = new FileInputStream(f);
            		} else {
	            		if (keyAndTrustStore.startsWith("/"))
	            			keyAndTrustStore = keyAndTrustStore.substring(1, keyAndTrustStore.length());
	            		f = new File(keyAndTrustStore);
            			keystoreStream = new FileInputStream(f);
            		}
            	}
            	if (keystoreStream == null) // still null ?
            		throw new IOException("Unable to load keystore from classpath/path: " + keyAndTrustStore);
            }
            
            keystore.load(keystoreStream, passphrase);
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, passphrase);
        	final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);
            final SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            return  ctx;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Creates an SSLSocketFactory. Pass a KeyStore resource with your
     * certificate and pass-phrase. Parameters read from configuration. 
	 * 
	 * @return SSL socket factory
	 * @throws IOException IO exception
	 */
    public static SSLSocketFactory makeSSLSocketFactory() throws Exception {
    	return makeSSLSocketFactory(getKeystoreFile(), getKeystorePw());
    }
    
    /**
     * Creates an SSLSocketFactory. Pass a KeyStore resource with your
     * certificate and pass-phrase.
	 * 
	 * @param keyAndTrustStore key-store file class-path reference or full path
	 * @param passphrase pass phrase
	 * @return SSL socket factory
	 * @throws IOException IO exception
	 */
    public static SSLSocketFactory makeSSLSocketFactory(String keyAndTrustStore, char passphrase[]) throws IOException {
        try {
        	return makeSSLContext(keyAndTrustStore, passphrase).getSocketFactory();
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Creates an SSLServerSocketFactory. Pass a KeyStore resource with your
     * certificate and pass-phrase. Parameters read from configuration.
	 * 
	 * @return SSL server socket factory
	 * @throws IOException IO exception
	 */
    public static SSLServerSocketFactory makeSSLServerSocketFactory() throws Exception {
    	return makeSSLServerSocketFactory(getKeystoreFile(), getKeystorePw());
    }
    
    /**
     * Creates an SSLServerSocketFactory. Pass a KeyStore resource with your
     * certificate and pass-phrase.
	 * 
	 * @param keyAndTrustStore key-store file class-path reference or full path
	 * @param passphrase pass phrase
	 * @return SSL server socket factory
	 * @throws IOException IO exception
	 */
    public static SSLServerSocketFactory makeSSLServerSocketFactory(String keyAndTrustStore, char passphrase[]) throws IOException {
        try {
            return makeSSLContext(keyAndTrustStore, passphrase).getServerSocketFactory();
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }
    
    /**
     * Creates an SSL HTTP client. Pass a KeyStore resource with your
     * certificate and pass-phrase. Don't forget to close the client if it has been used.
	 * 
	 * @param keyAndTrustStore key-store file class-path reference or full path
	 * @param passphrase pass phrase
	 * @param config request configuration phrase
	 * @return SSL HTTP Client
	 * @throws IOException IO exception
	 */
    public static CloseableHttpClient makeSSLHttpClient(String keyAndTrustStore, char passphrase[], RequestConfig config) throws IOException {
    
    	if (httpSslSf == null) {
    	
	    	try {
	            final KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	            InputStream keystoreStream = SSL.class.getResourceAsStream(keyAndTrustStore);
	            
	            if (keystoreStream == null) {
	            	if (keyAndTrustStore != null) {
	            		// get as file
	            		File f = new File(keyAndTrustStore);
	            		if (f.exists()) {
	            			keystoreStream = new FileInputStream(f);
	            		} else {
		            		if (keyAndTrustStore.startsWith("/"))
		            			keyAndTrustStore = keyAndTrustStore.substring(1, keyAndTrustStore.length());
		            		f = new File(keyAndTrustStore);
	            			keystoreStream = new FileInputStream(f);
	            		}
	            	}
	            	if (keystoreStream == null) // still null ?
	            		throw new IOException("Unable to load keystore from classpath/path: " + keyAndTrustStore);
	            }
	            
	            keystore.load(keystoreStream, passphrase);
	
	            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
	            keyManagerFactory.init(keystore, passphrase);
	        	final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	            trustManagerFactory.init(keystore);
	            
	            final String tlsProts = System.getProperty("https.protocols");
	            SSLContext ctx = null;
	            if (tlsProts.indexOf("TLSv1.3") > 0)
	            	ctx = SSLContext.getInstance("TLSv1.3");
	            else
	            	ctx = SSLContext.getInstance("TLSv1.2");
	            ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
	            
	            if (verifyHost)
	            	httpSslSf = new SSLConnectionSocketFactory(ctx, new DefaultHostnameVerifier());
	            else
	            	httpSslSf = new SSLConnectionSocketFactory(ctx, NoopHostnameVerifier.INSTANCE);
	            
	    	    httpSocketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
	    	    			.register("https", httpSslSf)
	    	    			.register("http", new PlainConnectionSocketFactory())
	    	    			.build();
	    	    
	        } catch (Exception e) {
	            throw new IOException(e.getMessage(), e);
	        }    	    
    	}

    	// Get a fresh client.
		final BasicHttpClientConnectionManager httpConnectionManager = new BasicHttpClientConnectionManager(httpSocketFactoryRegistry);
	    return HttpClients.custom()
	    		.setSSLSocketFactory(httpSslSf)
	    		.setConnectionManager(httpConnectionManager)
	    		.setDefaultRequestConfig(config).build();
    }
    
}
