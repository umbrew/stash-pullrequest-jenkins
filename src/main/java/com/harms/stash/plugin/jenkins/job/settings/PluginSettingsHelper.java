package com.harms.stash.plugin.jenkins.job.settings;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.harms.stash.plugin.jenkins.job.settings.servlet.ManualTriggerBuildServlet;


public class PluginSettingsHelper {
    private static final Logger log = LoggerFactory.getLogger(ManualTriggerBuildServlet.class);
    
    private static final String CHECKED = "checked";
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
    public static final String DISABLE_AUTOMATIC_BUILD_BY_DEFAULT = PLUGIN_STORAGE_KEY + ".disableAutomaticBuildByDefault";
    private static final String JENKINS_PR_URL_FIELD = PLUGIN_STORAGE_KEY + ".jenkinsPRUrl";
    
    private static final String BUILD_DELAY_FIELD = PLUGIN_STORAGE_KEY + ".buildDelayField";
    
    public static final String PLUGIN_VERISON = PLUGIN_STORAGE_KEY + ".pluginVersion";
    
    private static Map<String, Calendar> jobScheduleDate = new ConcurrentHashMap<String, Calendar>();
    
    /**
     * Return the pull-request settings key. 
     * @param repositoryKey - The repository key
     * @param slug - The slug of the repository to search for
     * @param pullRequestId - The id of the pull-request
     * @return "stash.plugin.jenkins.settingsui.<project key>/<slug>/pull-request-id"
     */
    static private String getDisableAutomaticBuildSettingsKey(String projectKey, String slug, Long pullRequestId) {
        return "stash.plugin.jenkins.settingsui." + projectKey + "/" + slug+ "/" +pullRequestId;
    }
    
    static public String getPluginKey(String baseKey, String slug) {
        return baseKey+slug;
    }
    
    /**
     * Clear the automatic build flag for the specified repository, slug and pull request
     * @param projectKey - the id of the repository
     * @param slug - the slug
     * @param pullRequestId - the id of the pull request
     * @param settings - {@link PluginSettings}
     */
    static public void clearAutomaticBuildFlag(String projectKey, String slug, Long pullRequestId, PluginSettings settings) {
        settings.remove(getDisableAutomaticBuildSettingsKey(projectKey,slug, pullRequestId));
    }
    
    /**
     * Set the automatic build flag in the settings for the specified repository, slug and pull request
     * @param projectKey - the id of the repository
     * @param slug - the slug
     * @param pullRequestId - the id of the pull request
     * @param settings - {@link PluginSettings}
     */
    public static void enableAutomaticBuildFlag(String projectKey, String slug, Long pullRequestId, PluginSettings settings) {
        settings.put(getDisableAutomaticBuildSettingsKey(projectKey,slug, pullRequestId), CHECKED);
    }
    
    /**
     * Test if the automatic build is disable for the pull-request
     * @param projectKey - the id of the repository
     * @param slug - the slug
     * @param pullRequestId - the id of the pull request
     * @param settings - {@link PluginSettings}
     * @return true if the automatic build is disabled
     */
    public static boolean isAutomaticBuildDisabled(String projectKey, String slug, Long pullRequestId,PluginSettings settings) {
        return (CHECKED.equals(settings.get(PluginSettingsHelper.getDisableAutomaticBuildSettingsKey(projectKey,slug,pullRequestId))));
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
            byte[] password = new byte[0];
            if (settings.get(key) != null) {
                password = CryptoHelp.decrypt(CryptoHelp.getComputedKey(), ((String)settings.get(key)).getBytes());
            }
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
            byte[] username = new byte[0];
            if (settings.get(key) != null) {
                username = CryptoHelp.decrypt(CryptoHelp.getComputedKey(), ((String)settings.get(key)).getBytes());
            }
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
    
    
    /**
     * Return true if the disable build by default is enabled for the plug-in
     * @param slug
     * @param settings
     * @return
     */
    public static boolean isDisableAutomaticBuildByDefault(String slug, PluginSettings settings) {
        return (CHECKED.equals(settings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.DISABLE_AUTOMATIC_BUILD_BY_DEFAULT,slug))));
    }
    
    /**
     * Return true if the trigger on create flag is enabled for the plug-in
     * @param slug
     * @param settings
     * @return
     */
    public static boolean isTriggerBuildOnCreate(String slug, PluginSettings settings) {
        return (CHECKED.equals(settings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_CREATE,slug))));
    }
    
    /**
     * Return true if the trigger on reopen build flag is enabled for the plug-in
     * @param slug
     * @param settings
     * @return
     */
    public static boolean isTriggerBuildOnReopen(String slug, PluginSettings settings) {
        return (CHECKED.equals(settings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_REOPEN,slug))));
    }
    
    /**
     * Return true if the trigger on update flag is enabled for the plug-in
     * @param slug
     * @param settings
     * @return
     */
    public static boolean isTriggerBuildOnUpdate(String slug, PluginSettings settings) {
        return (CHECKED.equals(settings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_UPDATE,slug))));
    }
    
    /**
     * Return the name of the build ref field on the settings. This point to a parameter on the Jenkins Job
     * @param slug
     * @param settings
     * @return
     */
    public static String getBuildReferenceField(String slug, PluginSettings settings) {
        String buildRefField = (String) settings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_REF_FIELD,slug));
        return buildRefField == null  ? "" : buildRefField;
    }
    
    /**
     * Set the name of the build ref field on the settings. This point to a parameter on the Jenkins Job
     * @param slug
     * @param buildRef
     * @param settings
     */
    public static void setBuildReferenceField(String slug, String buildRef, PluginSettings settings) {
        settings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_REF_FIELD,slug), buildRef);
    }
    
    /**
     * Return the build trigger delay from on the settings. This point to a parameter on the Jenkins Job
     * if no previous value is specified it will default to 300 seconds
     * @param slug
     * @param settings
     * @return
     */
    public static Integer getBuildDelay(String slug, PluginSettings settings) {
        Integer buildDelayField = 300;
        if (settings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_DELAY_FIELD,slug)) != null) {
            buildDelayField = Integer.valueOf((String)settings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_DELAY_FIELD,slug)));
        }
        return buildDelayField;
    }
    
    /**
     * Set the build trigger delay on the settings. This point to a parameter on the Jenkins Job
     * @param slug
     * @param buildDelay
     * @param settings
     */
    public static void setBuildDelay(String slug, Integer buildDelay, PluginSettings settings) {
        settings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_DELAY_FIELD,slug), buildDelay.toString());
    }
    
    /**
     * Set the name of the build titel field on the settings. This point to a parameter on the Jenkins Job
     * @param slug
     * @param buildTitle
     * @param settings
     */
    public static void setBuildTitleField(String slug, String buildRef, PluginSettings settings) {
        settings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_TITLE_FIELD,slug), buildRef);
    }
    
    
    /**
     * Return the name of the title field from the settings 
     * @param slug
     * @param settings
     * @return
     */
    public static String getBuildTitleField(String slug, PluginSettings settings) {
       String buildTitleField = (String) settings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_TITLE_FIELD,slug));
       return buildTitleField == null ? "" : buildTitleField;
    }
    
    /**
     * Returne the list of Jenkins CI servers from the settings
     * @param slug
     * @param settings
     * @return a array of server or server[0] if any added
     */
    public static String[] getJenkinsCIServerList(String slug, PluginSettings settings) {
        String[] serverList = new String[0];
        String ciServerList = PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_CI_SERVER_LIST,slug);
        if (settings.get(ciServerList) != null){
            serverList = ((String)settings.get(ciServerList)).split(",");
            for (int i = 0; i < serverList.length; i++) {
                serverList[i] = serverList[i].trim();
            }
        }
        return serverList;
    }
    
    /**
     * Return the last used Jenkins CI server
     * @param slug
     * @param settings
     * @return
     */
    public static String getLastJenkinsCIServer(String  slug, PluginSettings settings) {
        return (String) settings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_LAST_CI_SERVER,slug));
    }
    
    /**
     * Store the last used Jenkins CI server in settings storage 
     * @param slug
     * @param lastCiServer
     * @param settings
     */
    public static void setLastJenkinsCIServer(String slug, String lastCiServer, PluginSettings settings) {
        settings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_LAST_CI_SERVER,slug), lastCiServer);
    }
    
    /**
     * Enable the disable build by default
     * @param slug
     * @param settings
     */
    public static void enableDisableAutomaticBuildByDefault(String slug, PluginSettings settings) {
        settings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.DISABLE_AUTOMATIC_BUILD_BY_DEFAULT, slug), CHECKED);
    }
    /**
     * Enable it should trigger a build when a pull-request is created
     * @param slug
     * @param settings
     */
    public static void enableTriggerOnCreate(String slug, PluginSettings settings) {
        settings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_CREATE, slug), CHECKED);
    }

    /**
     * Enable it should trigger a build when the pull-request is updated
     * @param slug
     * @param settings
     */
    public static void enableTriggerOnUpdate(String slug, PluginSettings settings) {
        settings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_UPDATE, slug), CHECKED);

    }

    /**
     * Enable it should trigger a build when a pull-request is open
     * @param slug
     * @param settings
     */
    public static void enableTriggerOnReopen(String slug, PluginSettings settings) {
        settings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_REOPEN, slug), CHECKED);
    }
    
    /**
     * Set the list of Jenkins server on the settings.
     * @param jenkinsCIServerList - A list of jenkins servers
     * @param slug
     * @param settings
     */
    public static void setJenkinsCIServerList(String[] jenkinsCIServerList, String slug, PluginSettings settings) {
        String concatString = Arrays.toString(jenkinsCIServerList).trim();
        settings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_CI_SERVER_LIST,slug), concatString.substring(1, concatString.length()-1));

    }
    
    /**
     * Reset the Plug-in settings
     * @param slug - Repository Key
     * @param pluginSettings
     */
    public static void resetSettings(String slug, PluginSettings pluginSettings) {
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_USERNAME,slug));
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PASSWORD,slug));
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_TITLE_FIELD,slug));
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_REF_FIELD,slug));
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PR_URL_FIELD,slug));
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_DELAY_FIELD,slug));
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_CREATE,slug));
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_UPDATE,slug));
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_REOPEN,slug));
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_CI_SERVER_LIST,slug));
        pluginSettings.remove(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_LAST_CI_SERVER,slug));
    }
    
    /**
     * Generate schedule time for when the build should be triggered
     * @param slug
     * @param plugin settings
     * @param pullRequest - the pull request the schedule date should be calculated for
     * @return {@link Date}
     */
    public static Date generateScheduleJobTime(String slug, PluginSettings settings, Long pullRequestId) {
       return setScheduleJobTime(slug, settings, pullRequestId, getBuildDelay(slug, settings));
    }
    
    /**
     * Return a schedule date time based on the specified delay in seconds
     * @param slug
     * @param settings - plug-in settings
     * @param pullRequestId - the pull request the schedule date should be calculated for
     * @param delay - the delay in seconds
     * @return {@link Date}
     */
    public static Date setScheduleJobTime(String slug, PluginSettings settings, Long pullRequestId, Integer delay) {
        String jobKey = getScheduleJobKey(slug,pullRequestId);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, delay);
        jobScheduleDate.put(jobKey, calendar);
        
        if (log.isDebugEnabled()) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            log.debug(String.format("Calculated execution time %s for job %s",sdf.format(calendar.getTime()),jobKey));
        }
        return calendar.getTime(); 
    }
    
    /**
     * Return the registered job time for the specified job 
     * @param jobId
     * @return
     */
    public static Calendar getScheduleJobTime(String jobId) {
        return jobScheduleDate.get(jobId);
    }
    /**
     * Return the job schedule key calculated based on the pull-request id
     * @param slug
     * @param pullRequestId
     * @return
     */
    public static String getScheduleJobKey(String slug, Long pullRequestId) {
       return slug+"."+pullRequestId;
    }
    
    /**
     * Remove the last job schedule time
     * @param jobKey
     */
    public static void resetScheduleTime(String jobKey) {
        log.debug("Remove registered job trigger time with key "+jobKey);
        jobScheduleDate.remove(jobKey);
    }
    
    /**
     * Return the build pull-request field name. This point to a parameter on the Jenkins Job
     * @param slug
     * @param settings
     * @return
     */
    public static String getPullRequestUrlFieldName(String slug, PluginSettings settings) {
        String prUrl = "";
        if (settings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PR_URL_FIELD,slug)) != null) {
            prUrl = (String) settings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PR_URL_FIELD,slug)); 
        }
        
        return prUrl;
    }
    
    /**
     * Set the build pull-request field name. This point to a parameter on the Jenkins Job
     * @param slug
     * @param buildDelay
     * @param settings
     */
    public static void setPullRequestUrlFieldName(String slug, String field, PluginSettings settings) {
        settings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_PR_URL_FIELD,slug), field);
    }
}
