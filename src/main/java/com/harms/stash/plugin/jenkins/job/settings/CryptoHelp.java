package com.harms.stash.plugin.jenkins.job.settings;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Encrypt and Decrypt a values base on it's key. 
 * @author fharms
 *
 */
public class CryptoHelp {

    private static final Logger log = LoggerFactory.getLogger(CryptoHelp.class);
    /**
     * @return a computed key based on the HW MAC address
     * @throws UnknownHostException
     * @throws SocketException
     */
    public static byte[] getComputedKey() throws UnknownHostException, SocketException {
        String returnAddr = "";
        Enumeration<NetworkInterface> ni = NetworkInterface.getNetworkInterfaces();
        if (ni != null) {
            while (ni.hasMoreElements()) {
                NetworkInterface networkInterface = ni.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    byte[] mac = networkInterface.getHardwareAddress();
                    if (mac != null) {
                        /*
                         * Extract each array of mac address and convert it to hexa with the following format 08-00-27-DC-4A-9E.
                         */
                        returnAddr = ""; //$NON-NLS-1$
                        for (int i = 0; i < mac.length; i++) {
                            returnAddr += String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        }
                        log.info("Mac address found "+returnAddr); //$NON-NLS-1$
                        break;
                    } else {
                        log.info("Mac address is not accessible, faling back to the hostname"); //$NON-NLS-1$
                        returnAddr = InetAddress.getLocalHost().getHostName();
                    }
                }
            }
        } else {
            log.info("Mac address is not accessible, falling back to the IP"); //$NON-NLS-1$
            returnAddr = InetAddress.getLocalHost().getHostName();
        }
        return Arrays.copyOfRange(returnAddr.getBytes(Charsets.UTF_8),0,16);
    }
    
    public static byte[] encrypt(byte[] key, byte[] value) throws GeneralSecurityException {
        if (key.length != 16) {
            throw new IllegalArgumentException("Invalid key size.");
        }
        IvParameterSpec iv = new IvParameterSpec(key);

        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        return Base64.encodeBase64(cipher.doFinal(value));
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
            byte[] original = cipher.doFinal(Base64.decodeBase64(value));
    
            return original;
        }
        return new byte[0];
    }
}
