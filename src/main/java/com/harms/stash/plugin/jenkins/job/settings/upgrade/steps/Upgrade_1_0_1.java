package com.harms.stash.plugin.jenkins.job.settings.upgrade.steps;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;
import com.harms.stash.plugin.jenkins.job.settings.upgrade.Upgrade;

/**
 * Upgrade the plugin settings from the old format to the new format tu support multiple 
 * repository settings
 * 
 * @author fharms
 *
 */
public class Upgrade_1_0_1 implements Upgrade {

    private static final String PLUGIN_STORAGE_KEY = "stash.plugin.jenkins.settingsui";
    protected static final String BUILD_TITLE_FIELD = PLUGIN_STORAGE_KEY + ".buildTitleField";
    protected static final String BUILD_REF_FIELD = PLUGIN_STORAGE_KEY + ".buildRefField";
    protected static final String JENKINS_PASSWORD = PLUGIN_STORAGE_KEY + ".jenkinsPassword";
    protected static final String JENKINS_USERNAME = PLUGIN_STORAGE_KEY + ".jenkinsUserName";
    protected static final String JENKINS_BASE_URL = PLUGIN_STORAGE_KEY + ".jenkinsBaseUrl";
    protected static final String TRIGGER_BUILD_ON_CREATE = PLUGIN_STORAGE_KEY + ".triggerBuildOnCreate";
    protected static final String TRIGGER_BUILD_ON_UPDATE = PLUGIN_STORAGE_KEY + ".triggerBuildOnUpdate";
    protected static final String TRIGGER_BUILD_ON_REOPEN = PLUGIN_STORAGE_KEY + ".triggerBuildOnReopen";
    private final String slug;
    
    public Upgrade_1_0_1(String slug) {
        this.slug = slug;
    }

    @Override
    public void perform(PluginSettings pluginSettings) {
        String slug = this.slug;
        String jenkinsBaseUrl = (String) pluginSettings.get(JENKINS_BASE_URL);
        String userName = (String) pluginSettings.get(JENKINS_USERNAME);
        String password = (String) pluginSettings.get(JENKINS_PASSWORD);
        String buildRefField = (String) pluginSettings.get(BUILD_REF_FIELD);
        String buildTitleField = (String) pluginSettings.get(BUILD_TITLE_FIELD);
        String triggerOnBuildCreate = (String) pluginSettings.get(TRIGGER_BUILD_ON_CREATE);
        String triggerOnBuildReopen = (String) pluginSettings.get(TRIGGER_BUILD_ON_REOPEN);
        String triggerOnBuildUpdate = (String) pluginSettings.get(TRIGGER_BUILD_ON_UPDATE);

        if (jenkinsBaseUrl != null) {
            pluginSettings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_BASE_URL,slug), jenkinsBaseUrl);
        }
        
        if (buildRefField != null) {
            pluginSettings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_REF_FIELD,slug), buildRefField);
        }    
        
        if (triggerOnBuildCreate != null && !triggerOnBuildCreate.isEmpty()) {
            pluginSettings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_CREATE,slug), "checked");
        }

        if (triggerOnBuildUpdate != null && !triggerOnBuildUpdate.isEmpty()) {
            pluginSettings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_UPDATE,slug), "checked");
        }

        if (triggerOnBuildReopen != null && !triggerOnBuildReopen.isEmpty()) {
            pluginSettings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_REOPEN,slug), "checked");
        }

        if (userName != null) {
            pluginSettings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,slug), userName);
        }
        
        if (password != null) {
            pluginSettings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,slug), password);
        }

        if (buildTitleField != null) {
            pluginSettings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_TITLE_FIELD,slug), buildTitleField);
        }
        
        removeOldSettings(pluginSettings);
    }
    
    private void removeOldSettings(PluginSettings pluginSettings) {
       pluginSettings.remove(JENKINS_BASE_URL);
        pluginSettings.remove(JENKINS_USERNAME);
        pluginSettings.remove(JENKINS_PASSWORD);
        pluginSettings.remove(BUILD_REF_FIELD);
        pluginSettings.remove(BUILD_TITLE_FIELD);
        pluginSettings.remove(TRIGGER_BUILD_ON_CREATE);
        pluginSettings.remove(TRIGGER_BUILD_ON_REOPEN);
        pluginSettings.remove(TRIGGER_BUILD_ON_UPDATE);
    }

    @Override
    public String getVersion() {
        return "1.0.1";
    }

}
