package com.harms.stash.plugin.jenkins.job.settings;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Handle the pushing PXE settings between the HTML form and the {@link PluginSettings}
 * @author fharms
 *
 */
final public class PxeSettingsImpl implements PxeSettings {

    private PluginSettings pluginSettings;

    public PxeSettingsImpl(PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
    }
    
    @Override
    public void updateSettings(String slug, Map<String, String[]> parameterMap) {
       PluginSettingsHelper.setPxeHostName(slug, (String) parameterMap.get("pxeHostName")[0], pluginSettings);
       PluginSettingsHelper.setPxeHostUrl(slug, (String) parameterMap.get("pxeHostUrl")[0], pluginSettings); 
       PluginSettingsHelper.setHostCheckerUrl(slug, (String) parameterMap.get("hostCheckerUrl")[0], pluginSettings);
    }
    
    @Override
    public Map<String, Object> readSettings(String slug) {
        Map<String, Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("pxeHostName", PluginSettingsHelper.getPxeHostName(slug, pluginSettings));
        parameterMap.put("pxeHostUrl", PluginSettingsHelper.getPxeHostUrl(slug, pluginSettings));
        parameterMap.put("hostCheckerUrl", PluginSettingsHelper.getHostCheckerUrl(slug, pluginSettings));
        return parameterMap;
    }
    
}
