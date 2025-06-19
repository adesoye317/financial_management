package com.financal.mgt.Financal.Management.util;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * The type Encryption helper.
 */
public class EncryptionHelper {

	private static final String DES_CIPHER = "DES";

	private static final String UTF8_KEY = "UTF8";

	private static final String PASS_PHRASE = "attahemmanaudlan";

    /**
     * The entry point of application.
     *
     * @param a the input arguments
     */
    public static void main(String[] a) {
		System.out.println(encrypt("Anuoluwa1@"));
	}

    /**
     * Encrypt string.
     *
     * @param msg the msg
     * @return the string
     */
    @SuppressWarnings("restriction")
	public static String encrypt(String msg) {
		try {
			KeySpec keySpec = new DESKeySpec(EncryptionHelper.PASS_PHRASE.getBytes());
			SecretKey key = SecretKeyFactory.getInstance(EncryptionHelper.DES_CIPHER).generateSecret(keySpec);
			Cipher ecipher = Cipher.getInstance(key.getAlgorithm());
			ecipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] utf8 = msg.getBytes(StandardCharsets.UTF_8);
			byte[] enc = ecipher.doFinal(utf8);
			return Base64.getEncoder().encodeToString(enc);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
        return null;
	}

    /**
     * Encrypt or decrypt.
     *
     * @param mode the mode
     * @param is   the is
     * @param os   the os
     * @throws Throwable the throwable
     */
    public static void encryptOrDecrypt(int mode, InputStream is, OutputStream os) throws Throwable {
		KeySpec keySpec = new DESKeySpec(EncryptionHelper.PASS_PHRASE.getBytes());
		SecretKey desKey = SecretKeyFactory.getInstance(EncryptionHelper.DES_CIPHER).generateSecret(keySpec);
		Cipher cipher = Cipher.getInstance(desKey.getAlgorithm());
		cipher.init(Cipher.ENCRYPT_MODE, desKey);
		if (mode == Cipher.ENCRYPT_MODE) {
			cipher.init(Cipher.ENCRYPT_MODE, desKey);
			CipherInputStream cis = new CipherInputStream(is, cipher);
			EncryptionHelper.doCopy(cis, os);
		} else if (mode == Cipher.DECRYPT_MODE) {
			cipher.init(Cipher.DECRYPT_MODE, desKey);
			CipherOutputStream cos = new CipherOutputStream(os, cipher);
			EncryptionHelper.doCopy(is, cos);
		}
	}

    /**
     * Do copy.
     *
     * @param is the is
     * @param os the os
     * @throws IOException the io exception
     */
    public static void doCopy(InputStream is, OutputStream os) throws IOException {
		byte[] bytes = new byte[64];
		int numBytes;
		while ((numBytes = is.read(bytes)) != -1) {
			os.write(bytes, 0, numBytes);
		}
		os.flush();
		os.close();
		is.close();
	}

    /**
     * Decrypt string.
     *
     * @param msg the msg
     * @return the string
     */
    public static String decrypt(String msg) {
		try {
			KeySpec keySpec = new DESKeySpec(EncryptionHelper.PASS_PHRASE.getBytes());
			SecretKey key = SecretKeyFactory.getInstance(EncryptionHelper.DES_CIPHER).generateSecret(keySpec);
			Cipher decipher = Cipher.getInstance(key.getAlgorithm());
			decipher.init(Cipher.DECRYPT_MODE, key);
			byte[] dec = Base64.getDecoder().decode(msg);
			byte[] utf8 = decipher.doFinal(dec);
			return new String(utf8, StandardCharsets.UTF_8);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}

    /**
     * Encode url string.
     *
     * @param url the url
     * @return the string
     */
    public static String encodeURL(String url) {
		try {
			return URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

    /**
     * Decode url string.
     *
     * @param url the url
     * @return the string
     */
    public static String decodeURL(String url) {
		try {
			return URLDecoder.decode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

    /**
     * Encrypt url string.
     *
     * @param menuUrl the menu url
     * @return the string
     */
    public static String encryptURL(String menuUrl) {
		String retVal = menuUrl;
		int paracnt = menuUrl.trim().indexOf("?");
		if (paracnt > 0) {
			String plainStringmenu = menuUrl.substring(0, paracnt);
			String plainString = (menuUrl.substring(paracnt + 1, menuUrl.trim().length()));
			String encryptedS = menuUrl.substring(paracnt + 1, menuUrl.trim().length());
			try {
				encryptedS = "_asha=" + EncryptionHelper.encodeURL(EncryptionHelper.encrypt(plainString));
			} catch (Exception fe) {
				System.out.println("MenuFile Problem: " + fe.getMessage());
			}
			menuUrl = plainStringmenu + "?" + encryptedS;
			retVal = menuUrl;
		}
		return retVal;
	}



}
