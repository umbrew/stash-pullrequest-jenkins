package com.harms.stash.plugin.jenkins.job.settings;


public class PluginSettingsHelper {
    public static final String PLUGIN_STORAGE_KEY = "stash.plugin.jenkins.settingsui";
    public static final String BUILD_TITLE_FIELD = PLUGIN_STORAGE_KEY + ".buildTitleField";
    public static final String BUILD_REF_FIELD = PLUGIN_STORAGE_KEY + ".buildRefField";
    public static final String JENKINS_PASSWORD = PLUGIN_STORAGE_KEY + ".jenkinsPassword";
    public static final String JENKINS_USERNAME = PLUGIN_STORAGE_KEY + ".jenkinsUserName";
    public static final String JENKINS_BASE_URL = PLUGIN_STORAGE_KEY + ".jenkinsBaseUrl";
    public static final String TRIGGER_BUILD_ON_CREATE = PLUGIN_STORAGE_KEY + ".triggerBuildOnCreate";
    public static final String TRIGGER_BUILD_ON_UPDATE = PLUGIN_STORAGE_KEY + ".triggerBuildOnUpdate";
    public static final String TRIGGER_BUILD_ON_REOPEN = PLUGIN_STORAGE_KEY + ".triggerBuildOnReopen";
    
    public static final String PLUGIN_VERISON = PLUGIN_STORAGE_KEY + ".pluginVersion";
    
    
    /**
     * Return the pull-request settings key. 
     * @param projectKey - The project key
     * @param slug - The 
     * @param pullRequestId - The id of the pull-request
     * @return "stash.plugin.jenkins.settingsui.<project key>/<slug>/pull-request-id"
     */
    static public String getDisableAutomaticBuildSettingsKey(String projectKey, String slug, String pullRequestId) {
        return "stash.plugin.jenkins.settingsui." + projectKey + "/" + slug+ "/" +pullRequestId;
    }
    
    static public String getPluginKey(String baseKey, String slug) {
        return baseKey+slug;
    }
}
