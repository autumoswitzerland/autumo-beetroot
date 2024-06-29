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

import java.lang.reflect.Constructor;
import java.security.MessageDigest;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.security.SecureApplication;
import ch.autumo.beetroot.security.password.PasswordHashProvider;


/**
 * Security helper methods. 
 */
public class Security {

	private final static Logger LOG = LoggerFactory.getLogger(Security.class.getName());
	
	/** Initialize HASH provider */
	private static PasswordHashProvider hashProvider = null;

	/**
	 * Hash a password with configured HASH algorithm implementation.
	 * Cannot be reversed.
	 * 
	 * @param password password to hash
	 * @return hashed password
	 * @throws UtilsException utilities exception
	 */
	public static String hashPw(String password) throws UtilsException {
		// initialize first?
		if (hashProvider == null)
			initializeHashProvider();
		
		try {
			return hashProvider.hash(password);
		} catch (Exception e) {
			throw new UtilsException("Can't hash password!", e);
		}
	}
	
	/**
	 * Verify password with configured HASH algorithm implementation.
	 * 
	 * @param password password to check
	 * @param hashedPassword hashed password previously loaded
	 * @return true if password match
	 * @throws UtilsException utilities exception
	 */
	public static boolean verifyPw(String password, String hashedPassword) throws UtilsException {
		// initialize first?
		if (hashProvider == null)
			initializeHashProvider();
		
		try {
			return hashProvider.verify(password, hashedPassword);
		} catch (Exception e) {
			throw new UtilsException("Can't hash password!", e);
		}
	}	
	
	/**
	 * Generates a CSRF token.
	 * 
	 * @param secureApplication a secure app is needed!
	 * @return The generated CSRF.
	 * @throws UtilsException utilities exception
	 */
	public static String generateCSRFToken(SecureApplication secureApplication) throws UtilsException {
		final String guid = GUIDGenerator.generate();
		return encodeBase64_PBE_MD5_DES(guid, secureApplication);
	}

	/**
	 * 
	 * Encode data. It is the algorithm 1.
	 * 
	 * @param data data
	 * @param secureApplication secure application
	 * @return encoded PW
	 * @throws UtilsException utilities exception
	 */
	public static String encode(String data, SecureApplication secureApplication) throws UtilsException {
		return encodeBase64_SHA3_256_AES(data, secureApplication);
	}

	/**
	 * 
	 * Decode data. It is the algorithm 1.
	 * 
	 * @param data data
	 * @param secureApplication secure application
	 * @return decoded PW
	 * @throws UtilsException utilities exception
	 */
	public static String decode(String data, SecureApplication secureApplication) throws UtilsException {
		return decodeBase64_SHA3_256_AES(data, secureApplication);
	}

	/**
	 * 
	 * Encode com data.
	 * 
	 * @param data data
	 * @param secureApplication secure application
	 * @return encoded data
	 * @throws UtilsException utilities exception
	 */
	public static String encodeCom(String data, SecureApplication secureApplication) throws UtilsException {
		return encodeBase64_SHA3_256_AES(data, secureApplication);
	}
	
	/**
	 * 
	 * Decode com data.
	 * 
	 * @param data data
	 * @param secureApplication secure application
	 * @return decoded data
	 * @throws UtilsException utilities exception
	 */
	public static String decodeCom(String data, SecureApplication secureApplication) throws UtilsException {
		return decodeBase64_SHA3_256_AES(data, secureApplication);
	}	
	
	private static void initializeHashProvider() throws UtilsException {
		final boolean db_pw_enc = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_DB_PW_ENC);
		final String impl = BeetRootConfigurationManager.getInstance().getString("hash_implementation");
		if (db_pw_enc && (impl == null || impl.length() == 0)) {
			LOG.error("No HASH implementation defined, but passwords must be encoded (db_pw_encoded=yes)!");
			throw new UtilsException("No HASH implementation defined, but passwords must be encoded (db_pw_encoded=yes)!");
		}
        Constructor<?> constructor;
		try {
			constructor = Class.forName(impl).getDeclaredConstructor();
	        constructor.setAccessible(true);
	        hashProvider = (PasswordHashProvider) constructor.newInstance();
		} catch (Exception e) {
			LOG.error("Can't instantiate HASH provider '"+impl+"'!");
			throw new UtilsException("Can't instantiate HASH provider '"+impl+"'!");
		}
	}
	
	
	// Internal encoding / decoding
	//------------------------------------------------------------------------------

	@SuppressWarnings("unused")
	private static String encodeBase64_SHA256_AES(String data, SecureApplication app) throws UtilsException {
		return encodeBase64_ANY_AES("SHA-256", data, app);
    }	
	
	@SuppressWarnings("unused")
	private static String decodeBase64_SHA256_AES(String data, SecureApplication app) throws UtilsException {
		return decodeBase64_ANY_AES("SHA-256", data, app);
    }
	
	private static String encodeBase64_SHA3_256_AES(String data, SecureApplication app) throws UtilsException {
		return encodeBase64_ANY_AES("SHA3-256", data, app);
    }	
	
	private static String decodeBase64_SHA3_256_AES(String data, SecureApplication app) throws UtilsException {
		return decodeBase64_ANY_AES("SHA3-256", data, app);
    }
	
	private static String encodeBase64_PBE_MD5_DES(String data, SecureApplication app) throws UtilsException {
    	
        try {
	        //Key generation for enc and desc
	        KeySpec keySpec = new PBEKeySpec(app.getUniqueSecurityKey().toCharArray(), KEYDATA.SALT_1, KEYDATA.ITER_1);
	        SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
	        // Prepare the parameter to the ciphers
	        AlgorithmParameterSpec paramSpec = new PBEParameterSpec(KEYDATA.SALT_1, KEYDATA.ITER_1);
	
	        //Enc process
	        Cipher ecipher = Cipher.getInstance(key.getAlgorithm());
	        ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
	        String charSet = "UTF-8";
	        byte[] in = data.getBytes(charSet);
	        byte[] out = ecipher.doFinal(in);
	        String encStr = new String(Base64.encodeBase64(out));
	        return encStr;
        } catch (Exception e) {
			throw new UtilsException("Couldn't encode password/key!", e);
        }
    }	
	
	@SuppressWarnings("unused")
	private static String decodeBase64_PBE_MD5_DES(String data, SecureApplication app) throws UtilsException {
    	
        try {
	        //Key generation for enc and desc
	        KeySpec keySpec = new PBEKeySpec(app.getUniqueSecurityKey().toCharArray(), KEYDATA.SALT_1, KEYDATA.ITER_1);
	        SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
	        // Prepare the parameter to the ciphers
	        AlgorithmParameterSpec paramSpec = new PBEParameterSpec(KEYDATA.SALT_1, KEYDATA.ITER_1);
	        //Decryption process; same key will be used for decr
	        Cipher dcipher = Cipher.getInstance(key.getAlgorithm());
	        dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
	        byte[] enc = Base64.decodeBase64(data);
	        byte[] utf8 = dcipher.doFinal(enc);
	        String charSet = "UTF-8";
	        String plainStr = new String(utf8, charSet);
	        return plainStr;
        } catch (Exception e) {
			throw new UtilsException("Couldn't decode password/key!", e);
        }
	}	
	
	private static String decodeBase64_ANY_AES(String algo, String data, SecureApplication app) throws UtilsException {
    	byte[] cipherData = null;
		try {
			byte key[] = (app.getUniqueSecurityKey()).getBytes("UTF-8");
	    	MessageDigest sha = MessageDigest.getInstance(algo);
	    	key = sha.digest(key);
	    	SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
	    	byte[] crypted;
	    	crypted = Base64.decodeBase64(data);
	    	Cipher cipher = Cipher.getInstance("AES");
	    	cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
	    	cipherData = cipher.doFinal(crypted);
		} catch (Exception e) {
			throw new UtilsException("Couldn't decode password/key!", e);
		}
    	return new String(cipherData);
    }
	
	private static String encodeBase64_ANY_AES(String algo, String data, SecureApplication app) throws UtilsException {
    	byte[] encrypted = null;
		try {
			byte key[] = (app.getUniqueSecurityKey()).getBytes("UTF-8");
	    	MessageDigest sha = MessageDigest.getInstance(algo);
	    	key = sha.digest(key);
	    	SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
	    	Cipher cipher = Cipher.getInstance("AES");
	    	cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
	    	encrypted = cipher.doFinal(data.getBytes());
		} catch (Exception e) {
			throw new UtilsException("Couldn't encode password/key!", e);
		}
    	return Base64.encodeBase64String(encrypted);
    }	
	
	/**
	 * Internal keys.
	 */
	public final static class KEYDATA {
		private static final byte[] SALT_1 = {
				(byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
				(byte) 0x56, (byte) 0x35, (byte) 0xE3, (byte) 0x03
			};
		public static final int LEN_3 = 16;	
		public static final int LEN_4 = 32;
		private static final int ITER_1 = 65536;
	}
	
}
