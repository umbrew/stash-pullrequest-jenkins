package com.harms.stash.plugin.jenkins.job.settings;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

final public class JenkinsSettingsImpl implements PxeSettings {
    private static final Logger log = LoggerFactory.getLogger(JenkinsSettingsImpl.class);
    private final PluginSettings ps;

    public JenkinsSettingsImpl(PluginSettingsFactory pluginSettingsFactory) {
        this.ps = pluginSettingsFactory.createGlobalSettings();
    }
    
    @Override
    public void updateSettings(String slug, Map<String, String[]> parameterMap) throws EncryptException {
        PluginSettingsHelper.resetSettings(slug,ps);
        
        setJenkinsCiServerList(parameterMap, slug);
        
        PluginSettingsHelper.setBuildReferenceField(slug, parameterMap.get("buildRefField")[0], ps);
        PluginSettingsHelper.setBuildDelay(slug, new Integer(parameterMap.get("buildDelayField")[0]), ps);
        
        enableTriggerOnCreate(parameterMap, slug);
        
        enableTriggerOnUpdate(parameterMap, slug);
        
        enableTriggerOnReopen(parameterMap, slug);
        
        setJenkinsUserName(parameterMap, slug);
        setJenkinsPassword(parameterMap, slug);

        setBuildTitleField(parameterMap, slug);
        
        setPullRequestUrlField(parameterMap, slug);
    }
    
    @Override
    public Map<String, Object> readSettings(String slug) {
        Map<String, Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("jenkinsCIServerList", PluginSettingsHelper.getJenkinsCIServerList(slug, ps));
        
        getJenkinsUsername(parameterMap, slug);
        
        getJenkinsPassword(parameterMap, slug);
        
        parameterMap.put("buildRefField", PluginSettingsHelper.getBuildReferenceField(slug, ps));
        parameterMap.put("buildDelayField", PluginSettingsHelper.getBuildDelay(slug, ps).toString());
        parameterMap.put("buildTitleField", PluginSettingsHelper.getBuildTitleField(slug, ps));
        parameterMap.put("buildPullRequestUrlField", PluginSettingsHelper.getPullRequestUrlFieldName(slug, ps));
        
        isTriggerOnCreate(parameterMap, slug); 
        
        isTriggerOnUpdate(parameterMap, slug); 
        
        isTriggerOnReopen(parameterMap, slug); 
        return parameterMap;
    }

    private void isTriggerOnReopen(Map<String, Object> context, String slug) {
        if (PluginSettingsHelper.isTriggerBuildOnReopen(slug, ps)){
            context.put("triggerBuildOnReopen", "checked=\"checked\"");
        }
    }

    private void isTriggerOnUpdate(Map<String, Object> context, String slug) {
        if (PluginSettingsHelper.isTriggerBuildOnUpdate(slug, ps)){
            context.put("triggerBuildOnUpdate", "checked=\"checked\"");
        }
    }

    private void isTriggerOnCreate(Map<String, Object> context, String slug) {
        if (PluginSettingsHelper.isTriggerBuildOnCreate(slug, ps)){
            context.put("triggerBuildOnCreate", "checked=\"checked\"");
        }
    }

    private void getJenkinsPassword(Map<String, Object> context, String slug) {
        try {
            context.put("jenkinsPassword", new String(PluginSettingsHelper.getPassword(slug, ps)));
        } catch (DecryptException e) {
            log.error("Not able to decrypt the password, will reset the field. You will have to re-enter password",e);
            context.put("jenkinsPassword","");
        }
    }

    private void getJenkinsUsername(Map<String, Object> context, String slug) {
        try {
            context.put("jenkinsUserName",new String(PluginSettingsHelper.getUsername(slug, ps)));
        } catch (DecryptException e) {
            log.error("Not able to decrypt the username, will reset the field. You will have to re-enter username",e);
            context.put("jenkinsUserName","");
        }
    }
    private void setJenkinsCiServerList(Map<String, String[]> parameterMap, String slug) {
        String[] jenkinsCIServerList = parameterMap.get("jenkinsCIServerList");
        if (jenkinsCIServerList != null) {
            PluginSettingsHelper.setJenkinsCIServerList(jenkinsCIServerList, slug, ps);
        }
    }

    private void setPullRequestUrlField(Map<String, String[]> parameterMap, String slug) {
        if (!parameterMap.get("buildPullRequestUrlField")[0].isEmpty()) {
            PluginSettingsHelper.setPullRequestUrlFieldName(slug, parameterMap.get("buildPullRequestUrlField")[0], ps);
        }
    }

    private void setBuildTitleField(Map<String, String[]> parameterMap, String slug) {
        if (!parameterMap.get("buildTitleField")[0].isEmpty()) {
            PluginSettingsHelper.setBuildTitleField(slug, parameterMap.get("buildTitleField")[0], ps);
        }
    }

    private void setJenkinsPassword(Map<String, String[]> parameterMap, String slug) throws EncryptException {
        if (!parameterMap.get("jenkinsPassword")[0].isEmpty()) {
            PluginSettingsHelper.setPassword(slug, parameterMap.get("jenkinsPassword")[0].getBytes(), ps);
        }
    }

    private void setJenkinsUserName(Map<String, String[]> parameterMap, String slug) throws EncryptException {
        if (!parameterMap.get("jenkinsUserName")[0].isEmpty()) {
            PluginSettingsHelper.setUsername(slug, parameterMap.get("jenkinsUserName")[0].getBytes(), ps);
        }
    }

    private void enableTriggerOnReopen(Map<String, String[]> parameterMap, String slug) {
        if (parameterMap.containsKey("triggerBuildOnReopen")) {
            PluginSettingsHelper.enableTriggerOnReopen(slug, ps);
        }
    }

    private void enableTriggerOnUpdate(Map<String, String[]> parameterMap, String slug) {
        if (parameterMap.containsKey("triggerBuildOnUpdate")) {
            PluginSettingsHelper.enableTriggerOnUpdate(slug, ps);
        }
    }

    private void enableTriggerOnCreate(Map<String, String[]> parameterMap, String slug) {
        if (parameterMap.containsKey("triggerBuildOnCreate")) {
            PluginSettingsHelper.enableTriggerOnCreate(slug, ps);
        }
    }

    
}
