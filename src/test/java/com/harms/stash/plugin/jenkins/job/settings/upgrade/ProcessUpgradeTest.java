package com.harms.stash.plugin.jenkins.job.settings.upgrade;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.steps.Upgrade_1_0_1;

@RunWith(MockitoJUnitRunner.class)
public class ProcessUpgradeTest {
    
    @Mock
    private PluginSettings pluginSettings;

    @Mock
    private PluginSettingsFactory pluginSettingsFactory;

    @Mock
    private Upgrade_1_0_1 intialUpgrade;
   
    private UpgradeService processUpgrade;
    
    @Before
    public void setUp() throws Exception {
        Mockito.when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        processUpgrade = new UpgradeService(pluginSettingsFactory);
        processUpgrade.addUpgradeStep(intialUpgrade);
    }
    
    @Test
    public void testUpgrade() {
        Mockito.when(intialUpgrade.getVersion()).thenReturn("1.0.1");
        processUpgrade.process();
        
        Mockito.verify(pluginSettings,Mockito.times(1)).get(PluginSettingsHelper.PLUGIN_VERISON);
        Mockito.verify(pluginSettings,Mockito.times(1)).put(PluginSettingsHelper.PLUGIN_VERISON,"1.0.1");
        Mockito.verify(intialUpgrade,Mockito.times(2)).getVersion();
        Mockito.verify(intialUpgrade,Mockito.times(1)).perform(pluginSettings);
    }
    
    @Test
    public void testUpgradeVersion() {
        Upgrade testUpgradeStep = Mockito.mock(Upgrade.class);
        Mockito.when(pluginSettings.get(PluginSettingsHelper.PLUGIN_VERISON)).thenReturn(null,"1.0.1");
        Mockito.when(intialUpgrade.getVersion()).thenReturn("1.0.1");
        Mockito.when(testUpgradeStep.getVersion()).thenReturn("1.0.0");
        
        processUpgrade.addUpgradeStep(testUpgradeStep);
        processUpgrade.process();
        
        Mockito.verify(pluginSettings,Mockito.times(2)).get(PluginSettingsHelper.PLUGIN_VERISON);
        Mockito.verify(pluginSettings,Mockito.times(1)).put(PluginSettingsHelper.PLUGIN_VERISON,"1.0.1");
        Mockito.verify(intialUpgrade,Mockito.times(2)).getVersion();
        Mockito.verify(intialUpgrade,Mockito.times(1)).perform(pluginSettings);
        Mockito.verify(testUpgradeStep,Mockito.times(1)).getVersion();
    }
    
    @Test
    public void testMultipleUpgradeSteps() throws Exception {
        Upgrade testUpgradeStep = Mockito.mock(Upgrade.class);
        Mockito.when(pluginSettings.get(PluginSettingsHelper.PLUGIN_VERISON)).thenReturn(null,"1.0.1");
        Mockito.when(intialUpgrade.getVersion()).thenReturn("1.0.1");
        Mockito.when(testUpgradeStep.getVersion()).thenReturn("1.0.2");
        
        processUpgrade.addUpgradeStep(testUpgradeStep);
        processUpgrade.process();
        
        Mockito.verify(pluginSettings,Mockito.times(2)).get(PluginSettingsHelper.PLUGIN_VERISON);
        Mockito.verify(pluginSettings,Mockito.times(1)).put(PluginSettingsHelper.PLUGIN_VERISON,"1.0.1");
        Mockito.verify(pluginSettings,Mockito.times(1)).put(PluginSettingsHelper.PLUGIN_VERISON,"1.0.2");
        
        Mockito.verify(intialUpgrade,Mockito.times(2)).getVersion();
        Mockito.verify(intialUpgrade,Mockito.times(1)).perform(pluginSettings);
        
        Mockito.verify(testUpgradeStep,Mockito.times(2)).getVersion();
        Mockito.verify(testUpgradeStep,Mockito.times(1)).perform(pluginSettings);
    }
    
}
