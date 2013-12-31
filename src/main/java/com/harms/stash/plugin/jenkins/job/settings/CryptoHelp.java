package com.harms.stash.plugin.jenkins.job.settings;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.Charsets;

import com.sun.jersey.core.util.Base64;

/**
 * Encrypt and Decrypt a values base on it's key. 
 * @author fharms
 *
 */
public class CryptoHelp {

    /**
     * @return a computed key based on the HW MAC address
     * @throws UnknownHostException
     * @throws SocketException
     */
    public static byte[] getComputedKey() throws UnknownHostException, SocketException {
        InetAddress ip = InetAddress.getLocalHost();
 
        NetworkInterface network = NetworkInterface.getByInetAddress(ip);
        String hardwareAddress = formatAddress(network.getHardwareAddress());
        return Arrays.copyOfRange(hardwareAddress.getBytes(Charsets.UTF_8),0,16);
    }
    
    private static String formatAddress(byte[] address) {
        if (address == null) {
          return null;
        }

        StringBuilder ret = new StringBuilder(address.length * 2);
        for (byte b : address) {
          if (ret.length() > 0) {
            ret.append('-');
          }

          String bs = Integer.toHexString(b & 0x000000FF).toUpperCase();
          if (bs.length() < 2) {
            ret.append('0');
          }
          ret.append(bs);
        }

        return ret.toString();
      }
    
    public static byte[] encrypt(byte[] key, byte[] value) throws GeneralSecurityException {
        if (key.length != 16) {
            throw new IllegalArgumentException("Invalid key size.");
        }
        IvParameterSpec iv = new IvParameterSpec(key);

        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        return Base64.encode(cipher.doFinal(value));
    }

    public static byte[] decrypt(byte[] key, byte[] value) throws GeneralSecurityException {
        if (Base64.isBase64(new String(value))) {
            if (key.length != 16) {
                throw new IllegalArgumentException("Invalid key size.");
            }
            IvParameterSpec iv = new IvParameterSpec(key);
            
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
    
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] original = cipher.doFinal(Base64.decode(value));
    
            return original;
        }
        return new byte[0];
    }
}
