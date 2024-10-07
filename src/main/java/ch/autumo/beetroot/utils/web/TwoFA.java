/**
 * 
 * Copyright (c) 2024 autumo Ltd. Switzerland, Michael Gasche
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
package ch.autumo.beetroot.utils.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.utils.UtilsException;
import ch.autumo.beetroot.utils.system.OS;
import de.taimos.totp.TOTP;

/**
 * 2-FA Google Authenticator.
 */
public class TwoFA {

	/**
	 * Generate a secret user key with specific length.
	 * Note: Only initially for every user once!
	 * 
	 * @return secret user key
	 */
	public static String createSecretUserKey() {
		return createSecretUserKey(Constants.SECRET_USER_KEY_DEFAULT_LEN);
	}		
	
	/**
	 * Generate a secret user key with specific length.
	 * Note: Only initially for every user once!
	 * 
	 * @param len length
	 * @return secret user key
	 */
	public static String createSecretUserKey(int len) {
	    final SecureRandom random = new SecureRandom();
	    final byte[] bytes = new byte[len];
	    random.nextBytes(bytes);
	    final Base32 base32 = new Base32();
	    return base32.encodeToString(bytes);
	}	

	/**
	 * Create 5-digit TOTP (time-based one-time password) code
	 * for a user secret key.
	 * 
	 * @param secretUserKey secret user key
	 * @return code
	 */
	public static String create6DigitTOTPCode(String secretUserKey) {
	    final Base32 base32 = new Base32();
	    final byte[] bytes = base32.decode(secretUserKey);
	    final String hexKey = Hex.encodeHexString(bytes);
	    return TOTP.getOTP(hexKey);
	}

	/**
	 * Create Google Authenticator bar code.
	 * 
	 * @param secretUserKey secret user key
	 * @param email email of user
	 * @return bar code
	 * @throws UtilsException utilities exception
	 */
	public static String getGoogleAuthenticatorBarCode(String secretUserKey, String email) throws UtilsException  {
	    try {
	    	final String issuer = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_WS_APP_NAME); // app name !
	        return "otpauth://totp/"
	                + URLEncoder.encode(issuer + ":" + email, "UTF-8").replace("+", "%20")
	                + "?secret=" + URLEncoder.encode(secretUserKey, "UTF-8").replace("+", "%20")
	                + "&issuer=" + URLEncoder.encode(issuer, "UTF-8").replace("+", "%20");
	    } catch (UnsupportedEncodingException e) {
	        throw new UtilsException("Couldn't create Google authenticator bar code!", e);
	    }
	}
	
	/**
	 * Create QR code.
	 * 
	 * @param barCodeData Google authenticator bar code.
	 * @param height height of QR code image
	 * @param width width of QR code image
	 * @return path to QR code image file
	 * @throws UtilsException utilities exception
	 */
	public static String createQRCode(String barCodeData, int height, int width) throws UtilsException {
		
	    BitMatrix matrix;
		try {
			matrix = new MultiFormatWriter().encode(barCodeData, BarcodeFormat.QR_CODE, width, height);
		} catch (WriterException e) {
			throw new UtilsException("Couldn't create QR matrix for bar code", e);
		}
		
		File png = null;
		String absPath = null;
		try {
			
			png = File.createTempFile("2FA-", ".png", new File(OS.getTemporaryDirectory()));
			png.deleteOnExit();
	        absPath = png.getAbsolutePath();
			final FileOutputStream out = new FileOutputStream(png);
	        MatrixToImageWriter.writeToStream(matrix, "png", out);
	        
	    } catch (Exception e) {
			throw new UtilsException("Couldn't write QR matrix to: '"+absPath+"'!", e);
		}		
		
		return absPath;
	}
	
}
