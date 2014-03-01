package com.harms.stash.plugin.jenkins.job.intergration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.event.api.EventListener;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.stash.event.pull.PullRequestDeclinedEvent;
import com.atlassian.stash.event.pull.PullRequestEvent;
import com.atlassian.stash.event.pull.PullRequestMergedEvent;
import com.atlassian.stash.event.pull.PullRequestOpenedEvent;
import com.atlassian.stash.event.pull.PullRequestReopenedEvent;
import com.atlassian.stash.event.pull.PullRequestRescopedEvent;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.repository.Repository;
import com.harms.stash.plugin.jenkins.job.settings.DecryptException;
import com.harms.stash.plugin.jenkins.job.settings.EncryptException;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;

public class JenkinsTriggerJobIntergration {
    private static final Logger log = LoggerFactory.getLogger(JenkinsTriggerJobIntergration.class);
    
    private static final String PULLREQUEST_EVENT_CREATED = "CREATED";
    private static final String PULLREQUEST_EVENT_SOURCE_UPDATED = "SOURCE UPDATED";
    private static final String PULLREQUEST_EVENT_REOPEN = "REOPEN";
    
    private final PullRequestService pullRequestService;
	private byte[] userName;
	private byte[] password;
	private String buildRefField;
	private String buildTitleField;
    private PluginSettingsFactory pluginSettingsFactory;

    private boolean triggerBuildOnCreate;

    private boolean triggerBuildOnUpdate;

    private boolean triggerBuildOnReopen;

    private volatile String[] serverList;

	public JenkinsTriggerJobIntergration(PullRequestService pullRequestService, PluginSettingsFactory pluginSettingsFactory) {
		this.pullRequestService = pullRequestService;
        this.pluginSettingsFactory = pluginSettingsFactory;
	}

	private String getDisableAutomaticBuildSettingsKey(PullRequestEvent pushEvent) {
        PullRequest pullRequest = pushEvent.getPullRequest();
        Repository repository = pullRequest.getToRef().getRepository();
        String key = PluginSettingsHelper.getDisableAutomaticBuildSettingsKey(repository.getProject().getKey(),repository.getSlug(),pullRequest.getId().toString());
        return key;
    }
	
	/**
	 * Remove the disable automatic build settins when the pull-request is merged or declined
	 * @param pushEvent
	 */
	private void removeDisableAutomaticBuildProperty(PullRequestEvent pushEvent) {
	    String key = getDisableAutomaticBuildSettingsKey(pushEvent);
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        pluginSettings.remove(key);
	}

	/**
	 * Test if the automatic build is disable for the pull-request
	 * @param pushEvent - true if the build is disable
	 */
	private boolean isAutomaticBuildDisabled(PullRequestEvent pushEvent) {
	    String key = getDisableAutomaticBuildSettingsKey(pushEvent);
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        return (pluginSettings.get(key) != null);
	}
	
	/**
	 * Load the plug-in settings
	 * @param slug - slug id 
	 * @throws EncryptException 
	 */
	private void loadPluginSettings(String slug) throws DecryptException {
	    PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
	    
	    buildTitleField = "";
	    triggerBuildOnCreate = false;
	    triggerBuildOnReopen = false;
	    triggerBuildOnUpdate = false;
	    
        userName = PluginSettingsHelper.getUsername(slug, pluginSettings);
        password = PluginSettingsHelper.getPassword(slug, pluginSettings);
        
        buildRefField = (String) pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_REF_FIELD,slug));

        if (pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_TITLE_FIELD,slug)) != null){
           buildTitleField = (String) pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.BUILD_TITLE_FIELD,slug));
        }
        triggerBuildOnCreate = (pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_CREATE,slug)) != null);
        triggerBuildOnReopen = (pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_REOPEN,slug)) != null);
        triggerBuildOnUpdate = (pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.TRIGGER_BUILD_ON_UPDATE,slug)) != null);
	}
	
	/**
	 * @return the next CI server from the list of servers
	 */
	private String nextCIServer(String slug) {
	    String lastCiServer = null;
	    PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
	    synchronized (pluginSettings) {
	        
	        String ciServerList = PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_CI_SERVER_LIST,slug);
            serverList = ((String)pluginSettings.get(ciServerList)).split(",");
	        
	        lastCiServer = serverList[0];
    	    
	        String lastStoredCiServer = (String) pluginSettings.get(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_LAST_CI_SERVER,slug));
    	    if (lastStoredCiServer != null) {
    	        for (int i = 0; i < serverList.length; i++) {
                   if (serverList[i].equals(lastStoredCiServer)) {
                       if (i+1 < serverList.length) {
                           lastCiServer = serverList[i+1];
                       } 
                       break;
                   }
                }
    	    }
    	    lastCiServer = getBaseUrl(lastCiServer);
    	    log.info(String.format("select the next CI server from the list %s", lastCiServer));
    	    pluginSettings.put(PluginSettingsHelper.getPluginKey(PluginSettingsHelper.JENKINS_LAST_CI_SERVER,slug), lastCiServer);
	    }
	    return lastCiServer;
	}
	
	private boolean validateSettings(String jenkinsBaseUrl) {
	    return (jenkinsBaseUrl != null) && (buildRefField != null);
	}
	
    private void triggerBuild(PullRequestEvent pushEvent, int retryCount, String baseUrl) {
        String url = "";
        HttpResponse response = null;

        PullRequest pr = pushEvent.getPullRequest();
        try {
           
            url = buildJenkinsUrl(pr, baseUrl);
            HttpPost post = new HttpPost(url);
            
            response = httpClientRequest(post, userName, password);
            EntityUtils.consume(response.getEntity());
        } catch (Exception e) {
            if (!retryTriggerJob(pushEvent, retryCount, url,-1,e.getMessage())) {
                addErrorComment(pushEvent, String.format("Failed to trigger build %s\nException : %s",url,e.getMessage()));
                throw new RuntimeException(e);
            }
        } finally {
            if (response != null) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 400) {
                    if (!retryTriggerJob(pushEvent, retryCount, url, statusCode,"")) {
                        addErrorComment(pushEvent, String.format("All CI servers failed, no job is triggered",url));
                        throw new RuntimeException("All CI servers failed, no job is triggered\nFailed : HTTP error code : " + statusCode);
                    }
                } else {
                    addComment(pushEvent, response, baseUrl);
                }
            }
        }
    }

    private boolean retryTriggerJob(PullRequestEvent pushEvent, int retryCount, String url, int status, String errorText)  {
        String baseUrl;
        //try the next server in case of an error
        if (retryCount++ < serverList.length-1) {
            String comment = String.format("Try next CI server in the list, failed to call %s(%s)\n%s",url,status,errorText);
            addErrorComment(pushEvent, comment);
            baseUrl = nextCIServer(pushEvent.getPullRequest().getFromRef().getRepository().getSlug());
            triggerBuild(pushEvent, retryCount, baseUrl);
            return true;
        }
        return false;
    }

    /**
     * Build up the URL for trigger job on Jenkins with the specified parameters
     * @param pr - the pull-request
     * @return - A correct formatted URL for trigger a Jenkins job
     * @throws UnsupportedEncodingException
     */
    private String buildJenkinsUrl(PullRequest pr, String jenkinsBaseUrl) throws UnsupportedEncodingException {
        String url;
        String refId = String.format("%s=%s", buildRefField, URLEncoder.encode(pr.getFromRef().getLatestChangeset(), "utf-8"));
        @SuppressWarnings("deprecation")
        String titleValue = URLEncoder.encode(String.format("pull-request #%s - %s", pr.getId(),pr.getTitle(), "utf-8"));
        String title = buildTitleField == null || buildTitleField.isEmpty() ? "" : String.format("&%s=%s", buildTitleField, titleValue);

        url = jenkinsBaseUrl + "buildWithParameters?" + refId +title;
        return url.trim();
    }

    private String getBaseUrl(String jenkinsBaseUrl) {
        String baseUrl = jenkinsBaseUrl.toUpperCase().startsWith("HTTP") ? jenkinsBaseUrl : "http://" + jenkinsBaseUrl;
        baseUrl = jenkinsBaseUrl.lastIndexOf('/') == jenkinsBaseUrl.length()-1 ? jenkinsBaseUrl : jenkinsBaseUrl + "/";
        return baseUrl;
    }

    /**
     * Add a general comment to the pull-request with information about the commit id and link to the job
     * @param pushEvent
     * @param response 
     */
    private void addComment(PullRequestEvent pushEvent, HttpResponse response, String jenkinsBaseUrl) {
        String eventType = PULLREQUEST_EVENT_CREATED;
        if (pushEvent instanceof PullRequestRescopedEvent) {
           eventType = PULLREQUEST_EVENT_SOURCE_UPDATED;
        } else if (pushEvent instanceof PullRequestReopenedEvent) {
           eventType = PULLREQUEST_EVENT_REOPEN;
        }
        String comment = String.format("Build triggered\nEvent: %s\nCommit id: %s\nJob: %s",eventType,pushEvent.getPullRequest().getFromRef().getLatestChangeset(),jenkinsBaseUrl);
        pullRequestService.addComment(pushEvent.getPullRequest().getToRef().getRepository().getId(), pushEvent.getPullRequest().getId(), comment);
    }

    /**
     * Add a error message to the pull-request comment section
     * @param pushEvent
     * @param e - the {@link Exception}
     */
    private void addErrorComment(PullRequestEvent pushEvent, String comment) {
          pullRequestService.addComment(pushEvent.getPullRequest().getToRef().getRepository().getId(), pushEvent.getPullRequest().getId(), comment);
    }
    
    private HttpResponse httpClientRequest(HttpRequestBase request, byte[] userName, byte[] password) throws IOException, ClientProtocolException {
        DefaultHttpClient client;
        client = new DefaultHttpClient();

        BasicHttpContext context = new BasicHttpContext();

        if (userName != null && password != null) {
            client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), new UsernamePasswordCredentials(new String(userName), new String(password)));

            BasicScheme basicAuth = new BasicScheme();
            context.setAttribute("preemptive-auth", basicAuth);

            client.addRequestInterceptor((HttpRequestInterceptor) new PreemptiveAuth(), 0);
        }

        HttpResponse response = client.execute(request, context);
        return response;
    }
    
    @EventListener
    public void openPullRequest(PullRequestOpenedEvent pushEvent)
    {
        try {
            int retryCount = 0;
            String slug = pushEvent.getPullRequest().getFromRef().getRepository().getSlug();
            
            loadPluginSettings(slug);
            String jenkinsBaseUrl = nextCIServer(slug);
            
            if (triggerBuildOnCreate && validateSettings(jenkinsBaseUrl)) {
                triggerBuild(pushEvent, retryCount,jenkinsBaseUrl);
            }
        } catch (DecryptException e) {
            String comment = String.format("Error reading plug-in settings, please consult the logs for details %s",e.getMessage());
            addErrorComment(pushEvent, comment);
            log.error("Not able to read plug-in setting",e);
        }
    }
    
    @EventListener
    public void updatePullRequest(PullRequestRescopedEvent pushEvent)
    {
        try {
            int retryCount = 0;
            String slug = pushEvent.getPullRequest().getFromRef().getRepository().getSlug();
            
            loadPluginSettings(slug);
            String jenkinsBaseUrl = nextCIServer(slug);
            
            boolean isSourceChanged = !pushEvent.getPullRequest().getFromRef().getLatestChangeset().equals(pushEvent.getPreviousFromHash());
            
            if ((triggerBuildOnUpdate) && (!isAutomaticBuildDisabled(pushEvent)) && (validateSettings(jenkinsBaseUrl)) && (isSourceChanged)) {
                triggerBuild(pushEvent, retryCount, jenkinsBaseUrl);
            }
        } catch (DecryptException e) {
            String comment = String.format("Error reading plug-in settings, please consult the logs for details %s",e.getMessage());
            addErrorComment(pushEvent, comment);
            log.error("Not able to read plug-in setting",e);
        }
    }
    
    @EventListener
    public void reopenPullRequest(PullRequestReopenedEvent pushEvent)
    {
        try {
            int retryCount = 0;
            String slug = pushEvent.getPullRequest().getFromRef().getRepository().getSlug();
            
            loadPluginSettings(slug);
            String jenkinsBaseUrl = nextCIServer(slug);
            
            if (triggerBuildOnReopen && !isAutomaticBuildDisabled(pushEvent)) {
                triggerBuild(pushEvent, retryCount, jenkinsBaseUrl);
                
            }
        } catch (DecryptException e) {
            String comment = String.format("Error reading plug-in settings, please consult the logs for details %s",e.getMessage());
            addErrorComment(pushEvent, comment);
            log.error("Not able to read plug-in setting",e);
        }
    }
    
    @EventListener
    public void declinedPullRequest(PullRequestDeclinedEvent pushEvent)
    {
        //make sure we clean up the disable automatic property 
        removeDisableAutomaticBuildProperty(pushEvent);
    }
    
    @EventListener
    public void mergePullRequest(PullRequestMergedEvent pushEvent)
    {
        //make sure we clean up the disable automatic property 
        removeDisableAutomaticBuildProperty(pushEvent);
    }
    
    /**
     * Preemptive authentication interceptor
     *
     */
    static class PreemptiveAuth implements HttpRequestInterceptor {

        /*
         * (non-Javadoc)
         *
         * @see org.apache.http.HttpRequestInterceptor#process(org.apache.http.HttpRequest,
         * org.apache.http.protocol.HttpContext)
         */
        @SuppressWarnings("deprecation")
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            // Get the AuthState
            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

            // If no auth scheme available yet, try to initialize it preemptively
            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
                CredentialsProvider credsProvider = (CredentialsProvider) context
                        .getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authScheme != null) {
                    Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost
                            .getPort()));
                    if (creds == null) {
                        throw new HttpException("No credentials for preemptive authentication");
                    }
                    authState.setAuthScheme(authScheme);
                    authState.setCredentials(creds);
                }
            }

        }

    }

}
