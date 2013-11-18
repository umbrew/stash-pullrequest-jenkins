package com.harms.stash.plugin.jenkins.job.settings.upgrade.steps;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;
@RunWith(MockitoJUnitRunner.class)

public class Upgrade_1_0_1Test {

    private static final String SLUG01 = "slug01";

    @Mock
    private PluginSettings pluginSettings;
    
    private Upgrade_1_0_1 upgrade;

    @Before
    public void setUp() throws Exception {
        upgrade = new Upgrade_1_0_1(SLUG01);
    }

    @Test
    public void testPerform() {
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.JENKINS_BASE_URL)).thenReturn("baseurl");
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.JENKINS_USERNAME)).thenReturn("username");
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.JENKINS_PASSWORD)).thenReturn("password");
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.BUILD_REF_FIELD)).thenReturn("ref-field");
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.BUILD_TITLE_FIELD)).thenReturn("title");
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.TRIGGER_BUILD_ON_CREATE)).thenReturn("checked");
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.TRIGGER_BUILD_ON_REOPEN)).thenReturn("checked");
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.TRIGGER_BUILD_ON_UPDATE)).thenReturn("checked");
        
        upgrade.perform(pluginSettings);
        
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.JENKINS_BASE_URL);
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.JENKINS_USERNAME);
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.JENKINS_PASSWORD);
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.BUILD_REF_FIELD);
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.BUILD_TITLE_FIELD);
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.TRIGGER_BUILD_ON_CREATE);
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.TRIGGER_BUILD_ON_REOPEN);
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.TRIGGER_BUILD_ON_UPDATE);
        
        Mockito.verify(pluginSettings,Mockito.times(1)).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_BASE_URL,SLUG01), "baseurl");
        Mockito.verify(pluginSettings,Mockito.times(1)).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,SLUG01), "username");
        Mockito.verify(pluginSettings,Mockito.times(1)).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,SLUG01), "password");
        Mockito.verify(pluginSettings,Mockito.times(1)).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_REF_FIELD,SLUG01), "ref-field");
        Mockito.verify(pluginSettings,Mockito.times(1)).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_TITLE_FIELD,SLUG01), "title");
        Mockito.verify(pluginSettings,Mockito.times(1)).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_CREATE,SLUG01), "checked");
        Mockito.verify(pluginSettings,Mockito.times(1)).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_REOPEN,SLUG01), "checked");
        Mockito.verify(pluginSettings,Mockito.times(1)).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_UPDATE,SLUG01), "checked");
    }

    
    @Test
    public void testPerformNullValues() { 
        returnNullSettings();
        
        upgrade.perform(pluginSettings);
        
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.JENKINS_BASE_URL);
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.JENKINS_USERNAME);
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.JENKINS_PASSWORD);
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.BUILD_REF_FIELD);
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.BUILD_TITLE_FIELD);
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.TRIGGER_BUILD_ON_CREATE);
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.TRIGGER_BUILD_ON_REOPEN);
        Mockito.verify(pluginSettings,Mockito.times(1)).get(Upgrade_1_0_1.TRIGGER_BUILD_ON_UPDATE);
        
        Mockito.verify(pluginSettings,Mockito.never()).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_BASE_URL,SLUG01), null);
        Mockito.verify(pluginSettings,Mockito.never()).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,SLUG01), null);
        Mockito.verify(pluginSettings,Mockito.never()).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,SLUG01), null);
        Mockito.verify(pluginSettings,Mockito.never()).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_REF_FIELD,SLUG01), null);
        Mockito.verify(pluginSettings,Mockito.never()).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_TITLE_FIELD,SLUG01), null);
        Mockito.verify(pluginSettings,Mockito.never()).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_CREATE,SLUG01), null);
        Mockito.verify(pluginSettings,Mockito.never()).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_REOPEN,SLUG01), null);
        Mockito.verify(pluginSettings,Mockito.never()).put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_UPDATE,SLUG01), null);
    }

    
    
    @Test
    public void testPerformRemoveOldValues() { 
        returnNullSettings();
        upgrade.perform(pluginSettings);
        
        Mockito.verify(pluginSettings,Mockito.times(1)).remove(Upgrade_1_0_1.JENKINS_BASE_URL);
        Mockito.verify(pluginSettings,Mockito.times(1)).remove(Upgrade_1_0_1.JENKINS_USERNAME);
        Mockito.verify(pluginSettings,Mockito.times(1)).remove(Upgrade_1_0_1.JENKINS_PASSWORD);
        Mockito.verify(pluginSettings,Mockito.times(1)).remove(Upgrade_1_0_1.BUILD_REF_FIELD);
        Mockito.verify(pluginSettings,Mockito.times(1)).remove(Upgrade_1_0_1.BUILD_TITLE_FIELD);
        Mockito.verify(pluginSettings,Mockito.times(1)).remove(Upgrade_1_0_1.TRIGGER_BUILD_ON_CREATE);
        Mockito.verify(pluginSettings,Mockito.times(1)).remove(Upgrade_1_0_1.TRIGGER_BUILD_ON_REOPEN);
        Mockito.verify(pluginSettings,Mockito.times(1)).remove(Upgrade_1_0_1.TRIGGER_BUILD_ON_UPDATE);
    }

    private void returnNullSettings() {
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.JENKINS_BASE_URL)).thenReturn(null);
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.JENKINS_USERNAME)).thenReturn(null);
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.JENKINS_PASSWORD)).thenReturn(null);
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.BUILD_REF_FIELD)).thenReturn(null);
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.BUILD_TITLE_FIELD)).thenReturn(null);
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.TRIGGER_BUILD_ON_CREATE)).thenReturn(null);
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.TRIGGER_BUILD_ON_REOPEN)).thenReturn(null);
        Mockito.when(pluginSettings.get(Upgrade_1_0_1.TRIGGER_BUILD_ON_UPDATE)).thenReturn(null);
    }
    
    @Test
    public void testGetVersion() {
        assertEquals("1.0.1", upgrade.getVersion());
    }

}
