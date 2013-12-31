package com.harms.stash.plugin.jenkins.job.settings.upgrade.steps;

import static org.junit.Assert.assertEquals;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.harms.stash.plugin.jenkins.job.settings.CryptoHelp;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;
@RunWith(MockitoJUnitRunner.class)

public class Upgrade_1_0_2Test {

    private static final String MYPASSWORD = "MYPASSWORD";

    private static final String MYUSERNAME = "MYUSERNAME";

    private static final String SLUG01 = "slug01";

    @Mock
    private PluginSettings pluginSettings;
    
    private Upgrade_1_0_2 upgrade;

    @Before
    public void setUp() throws Exception {
        upgrade = new Upgrade_1_0_2(SLUG01);
    }

    @Test
    public void testPerform() throws UnknownHostException, SocketException, GeneralSecurityException {
        byte[] password = CryptoHelp.encrypt(CryptoHelp.getComputedKey(),MYPASSWORD.getBytes());
        byte[] username = CryptoHelp.encrypt(CryptoHelp.getComputedKey(),MYUSERNAME.getBytes());
        Mockito.when(pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,SLUG01))).thenReturn(MYUSERNAME);
        Mockito.when(pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,SLUG01))).thenReturn(MYPASSWORD);
        
        upgrade.perform(pluginSettings);
        
        Mockito.verify(pluginSettings,Mockito.times(1)).get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,SLUG01));
        Mockito.verify(pluginSettings,Mockito.times(1)).get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,SLUG01));
        
        Mockito.verify(pluginSettings,Mockito.times(1)).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,SLUG01), new String(username));
        Mockito.verify(pluginSettings,Mockito.times(1)).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,SLUG01), new String(password));
        
    }

    
    @Test
    public void testPerformNullValues() { 
        returnNullSettings();
        
        upgrade.perform(pluginSettings);
        
        Mockito.verify(pluginSettings,Mockito.times(1)).get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,SLUG01));
        Mockito.verify(pluginSettings,Mockito.times(1)).get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,SLUG01));
     
        Mockito.verify(pluginSettings,Mockito.never()).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,SLUG01), null);
        Mockito.verify(pluginSettings,Mockito.never()).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,SLUG01), null);
      
    }
    
    private void returnNullSettings() {
        Mockito.when(pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,SLUG01))).thenReturn(null);
        Mockito.when(pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,SLUG01))).thenReturn(null);
    
    }
    
    @Test
    public void testGetVersion() {
        assertEquals("1.0.2", upgrade.getVersion());
    }

}
