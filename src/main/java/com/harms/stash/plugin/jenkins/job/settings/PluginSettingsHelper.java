package com.harms.stash.plugin.jenkins.job.settings;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;

import com.atlassian.sal.api.pluginsettings.PluginSettings;


public class PluginSettingsHelper {
    public static final String PLUGIN_STORAGE_KEY = "stash.plugin.jenkins.settingsui";
    public static final String BUILD_TITLE_FIELD = PLUGIN_STORAGE_KEY + ".buildTitleField";
    public static final String BUILD_REF_FIELD = PLUGIN_STORAGE_KEY + ".buildRefField";
    public static final String JENKINS_PASSWORD = PLUGIN_STORAGE_KEY + ".jenkinsPassword";
    public static final String JENKINS_USERNAME = PLUGIN_STORAGE_KEY + ".jenkinsUserName";
    public static final String JENKINS_BASE_URL = PLUGIN_STORAGE_KEY + ".jenkinsBaseUrl";
    public static final String JENKINS_CI_SERVER_LIST = PLUGIN_STORAGE_KEY + ".jenkinsCIServerList";
    public static final String JENKINS_LAST_CI_SERVER = PLUGIN_STORAGE_KEY + ".jenkinsLastCIServer";
    public static final String TRIGGER_BUILD_ON_CREATE = PLUGIN_STORAGE_KEY + ".triggerBuildOnCreate";
    public static final String TRIGGER_BUILD_ON_UPDATE = PLUGIN_STORAGE_KEY + ".triggerBuildOnUpdate";
    public static final String TRIGGER_BUILD_ON_REOPEN = PLUGIN_STORAGE_KEY + ".triggerBuildOnReopen";
    
    public static final String PLUGIN_VERISON = PLUGIN_STORAGE_KEY + ".pluginVersion";
    
    
    /**
     * Return the pull-request settings key. 
     * @param projectKey - The project key
     * @param slug - The slug of the repository to search for
     * @param pullRequestId - The id of the pull-request
     * @return "stash.plugin.jenkins.settingsui.<project key>/<slug>/pull-request-id"
     */
    static public String getDisableAutomaticBuildSettingsKey(String projectKey, String slug, String pullRequestId) {
        return "stash.plugin.jenkins.settingsui." + projectKey + "/" + slug+ "/" +pullRequestId;
    }
    
    static public String getPluginKey(String baseKey, String slug) {
        return baseKey+slug;
    }
    
    /**
     * Get the decrypted password from the {@link PluginSettings}
     * @param slug - The slug of the repository to search for
     * @param settings - The plug-in settings
     * @return a the decrypted password in a byte array
     * @throws DecryptException
     */
    static public byte[] getPassword(String slug, PluginSettings settings) throws DecryptException {
        String key = getPluginKey(JENKINS_PASSWORD, slug);
        try {
            byte[] password = CryptoHelp.decrypt(CryptoHelp.getComputedKey(), ((String)settings.get(key)).getBytes());
            return password;
        } catch (UnknownHostException e) {
            throw new DecryptException("Not alble to get localhost",e);
        } catch (SocketException e) {
            throw new DecryptException(e);
        } catch (GeneralSecurityException e) {
            throw new DecryptException(e);
        }
    }
    
    /**
     * Encrypt and store the password for the plug-in. This is not bullet proof why of storing
     * password, but this at least make sure the it's not stored in free text.
     * @param slug - The slug of the repository to search for
     * @param password - The password to store 
     * @param settings - The plug-in settings
     * @throws EncryptException
     */
    static public void setPassword(String slug,byte[] password, PluginSettings settings) throws EncryptException {
        String key = getPluginKey(JENKINS_PASSWORD, slug);
        try {
            byte[] encryptedPassword = CryptoHelp.encrypt(CryptoHelp.getComputedKey(), password);
            settings.put(key, new String(encryptedPassword));
        } catch (UnknownHostException e) {
            throw new EncryptException("Not able to get localhost",e);
        } catch (SocketException e) {
            throw new EncryptException(e);
        } catch (GeneralSecurityException e) {
            throw new EncryptException(e);
        }
    }
    
    /**
     * Get the decrypted usename from the {@link PluginSettings}
     * @param slug - The slug of the repository to search for
     * @param settings - The plug-in settings
     * @return a the decrypted username in a byte array
     * @throws DecryptException
     */
    static public byte[] getUsername(String slug, PluginSettings settings) throws DecryptException {
        String key = getPluginKey(JENKINS_USERNAME, slug);
        try {
            byte[] username = CryptoHelp.decrypt(CryptoHelp.getComputedKey(), ((String)settings.get(key)).getBytes());
            return username;
        } catch (UnknownHostException e) {
            throw new DecryptException("Not alble to get localhost",e);
        } catch (SocketException e) {
            throw new DecryptException(e);
        } catch (GeneralSecurityException e) {
            throw new DecryptException(e);
        }
    }
    
    /**
     * Encrypt and store the username for the plug-in.This is not bullet proof why of storing
     * username, but this at least make sure it's not stored in free text.
     * @param slug - The slug of the repository to search for
     * @param username - The username to store 
     * @param settings - The plug-in settings
     * @throws EncryptException
     */
    static public void setUsername(String slug,byte[] username, PluginSettings settings) throws EncryptException {
        String key = getPluginKey(JENKINS_USERNAME, slug);
        try {
            byte[] encryptedUsername = CryptoHelp.encrypt(CryptoHelp.getComputedKey(), username);
            settings.put(key, new String(encryptedUsername));
        } catch (UnknownHostException e) {
            throw new EncryptException("Not alble to get localhost",e);
        } catch (SocketException e) {
            throw new EncryptException(e);
        } catch (GeneralSecurityException e) {
            throw new EncryptException(e);
        }
    }
}
