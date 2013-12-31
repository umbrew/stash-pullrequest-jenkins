package com.harms.stash.plugin.jenkins.job.settings;

import static org.junit.Assert.*;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;

import org.apache.commons.io.Charsets;
import org.junit.Test;

public class PasswordEncryptionHelpTest {

    @Test
    public void testGetComputedKey() throws UnknownHostException, SocketException {
        byte[] computedKey = CryptoHelp.getComputedKey();
        assertTrue(computedKey.length == 16); 
    }

    @Test
    public void testEncrypt() throws UnknownHostException, SocketException, GeneralSecurityException {
        String password = "MySecretPassword";
        byte[] encrypt = CryptoHelp.encrypt(CryptoHelp.getComputedKey(), password.getBytes());
        assertTrue(!password.equals(new String(encrypt)));
    }

    @Test
    public void testDecrypt() throws UnknownHostException, SocketException, GeneralSecurityException {
        String password = "MySecretPassword";
        byte[] encrypt = CryptoHelp.encrypt(CryptoHelp.getComputedKey(), password.getBytes(Charsets.UTF_8));
        byte[] decrypt = CryptoHelp.decrypt(CryptoHelp.getComputedKey(), encrypt);
        assertTrue(password.equals(new String(decrypt)));
    }

}
