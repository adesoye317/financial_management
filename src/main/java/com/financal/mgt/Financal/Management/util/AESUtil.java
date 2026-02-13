package com.financal.mgt.Financal.Management.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AESUtil {
    @Autowired
    private Environment env;


    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding"; // AES Algorithm/Mode/Padding
    private static final int TAG_LENGTH_BIT = 128; // Length of authentication tag
    private static final int IV_LENGTH_BYTE = 12; // Length of initialization vector
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    public  static String encryptTextUsingAES(String plainText, String aesKeyString) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(aesKeyString);
        byte[] iv = getRandomNonce(IV_LENGTH_BYTE);

        SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        Cipher aesCipher = Cipher.getInstance(ENCRYPT_ALGO);
        aesCipher.init(Cipher.ENCRYPT_MODE, originalKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] byteCipherText = aesCipher.doFinal(plainText.getBytes(UTF_8));
        // We prefix the IV to the encrypted text, because we need the same IV for Decryption
        byte[] cipherTextWithIv = ByteBuffer.allocate(iv.length + byteCipherText.length).put(iv).put(byteCipherText)
                .array();

        return Base64.getEncoder().encodeToString(cipherTextWithIv);
    }

    /*public static void main(String[] args) throws Exception {

        String data = "{\n" +
                "    \"email\": \"adettob@gmail.com\",\n" +
                "    \"password\": \"Testing1234\",\n" +
                "    \"deviceDetails\": {\n" +
                "        \"device_token\": \"erty\"\n" +
                "    }\n" +
                "}";
        String encrypt = encryptTextUsingAES(data, "tIPvEW6eWvHbkbj6nfUQACcol1zR9pD+zrvgveUlJCU=");// "uGyLeFi5B9akoY4GfvrhuxlvKrz9sr1b5oaylEqxEGQOQYbGDrKFtnmyCHlJuaefwkjsdCf+fG6Azu34yu+llmUgpkWLkPTtnAvMcgk=";
        String decrypt = decryptTextUsingAES(encrypt, "tIPvEW6eWvHbkbj6nfUQACcol1zR9pD+zrvgveUlJCU=");
        System.out.println("THE ENCRYPT::{}"+ encrypt);
//        System.out.println("THE DECRYPT::{}"+ decrypt);
    }*/



    public static String decryptTextUsingAES(String encryptedText, String aesKeyString) throws Exception {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(aesKeyString);
            SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

            byte[] decode = Base64.getDecoder().decode(encryptedText.getBytes(UTF_8));
            ByteBuffer bb = ByteBuffer.wrap(decode);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            bb.get(iv);
            byte[] cipherText = new byte[bb.remaining()];
            bb.get(cipherText);

            Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, originalKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, UTF_8);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    protected static byte[] getRandomNonce(int len) {
        byte[] nonce = new byte[len];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    public static void main(String[] args) throws Exception {

        String message = "xpenskey123@";
        String secret = "tIPvEW6eWvHbkbj6nfUQACcol1zR9pD+zrvgveUlJCU=";

        String encry = encryptTextUsingAES(message, secret);
        System.out.println("THE DATA::{}"+ encry);
    }
}
