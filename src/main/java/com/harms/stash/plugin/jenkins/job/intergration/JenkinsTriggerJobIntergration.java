package com.harms.stash.plugin.jenkins.job.intergration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.Header;
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
import org.apache.http.client.methods.HttpGet;
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
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;

public class JenkinsTriggerJobIntergration {
    private static final String NEXT_BUILD_NUMBER = "nextBuildNumber";

    private static final Logger log = LoggerFactory.getLogger(JenkinsTriggerJobIntergration.class);
    
    private static final String PULLREQUEST_EVENT_CREATED = "CREATED";
    private static final String PULLREQUEST_EVENT_SOURCE_UPDATED = "SOURCE UPDATED";
    private static final String PULLREQUEST_EVENT_REOPEN = "REOPEN";
    
    private final PullRequestService pullRequestService;
	private String jenkinsBaseUrl;
	private String userName;
	private String password;
	private String buildRefField;
	private String buildTitleField;
    private PluginSettingsFactory pluginSettingsFactory;

    private boolean triggerBuildOnCreate;

    private boolean triggerBuildOnUpdate;

    private boolean triggerBuildOnReopen;

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
	 */
	private void loadPluginSettings() {
	    PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
	    
	    buildTitleField = "";
	    triggerBuildOnCreate = false;
	    triggerBuildOnReopen = false;
	    triggerBuildOnUpdate = false;
	    
        jenkinsBaseUrl = (String) pluginSettings.get(PluginSettingsHelper.JENKINS_BASE_URL);
        
        userName = (String) pluginSettings.get(PluginSettingsHelper.JENKINS_USERNAME);
        password = (String) pluginSettings.get(PluginSettingsHelper.JENKINS_PASSWORD);
        
        buildRefField = (String) pluginSettings.get(PluginSettingsHelper.BUILD_REF_FIELD);

        if (pluginSettings.get(PluginSettingsHelper.BUILD_TITLE_FIELD) != null){
           buildTitleField = (String) pluginSettings.get(PluginSettingsHelper.BUILD_TITLE_FIELD);
        }
        triggerBuildOnCreate = (pluginSettings.get(PluginSettingsHelper.TRIGGER_BUILD_ON_CREATE) != null);
        triggerBuildOnReopen = (pluginSettings.get(PluginSettingsHelper.TRIGGER_BUILD_ON_REOPEN) != null);
        triggerBuildOnUpdate = (pluginSettings.get(PluginSettingsHelper.TRIGGER_BUILD_ON_UPDATE) != null);
	}
	
	private boolean validateSettings() {
	    return (jenkinsBaseUrl != null) && (buildRefField != null);
	}
	
    private void triggerBuild(PullRequestEvent pushEvent) {
        String url = "";
        HttpResponse response = null;
        String nextBuildNo = null;

        PullRequest pr = pushEvent.getPullRequest();
        try {
            String baseUrl = getBaseUrl();
            url = buildJenkinsUrl(pr);
            HttpGet getNextBuildNo = new HttpGet(baseUrl+"/api/json");
            
            //get the next build number. there is slide possibility this could happen concurrent with
            //another user trigger a job manual and then the job number does not match the job that is
            //triggered next.
            response = httpClientRequest(getNextBuildNo, userName, password);
            nextBuildNo = nextBuildNo(EntityUtils.toString(response.getEntity()));
            HttpPost post = new HttpPost(url);
            
            response = httpClientRequest(post, userName, password);
            EntityUtils.consume(response.getEntity());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (response.getStatusLine().getStatusCode() >= 400) {
                RuntimeException e = new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
                log.error("Error triggering: " + url, e);
                throw e;
            } else {
                addComment(pushEvent, response, nextBuildNo);
            }
        }
    }

    /**
     * Build up the URL for trigger job on Jenkins with the specified parameters
     * @param pr - the pull-request
     * @return - A correct formatted URL for trigger a Jenkins job
     * @throws UnsupportedEncodingException
     */
    private String buildJenkinsUrl(PullRequest pr) throws UnsupportedEncodingException {
        String url;
        String refId = String.format("%s=%s", buildRefField, URLEncoder.encode(pr.getFromRef().getLatestChangeset(), "utf-8"));
        @SuppressWarnings("deprecation")
        String titleValue = URLEncoder.encode(String.format("pull-request #%s - %s", pr.getId(),pr.getTitle(), "utf-8"));
        String title = buildTitleField == null || buildTitleField.isEmpty() ? "" : String.format("&%s=%s", buildTitleField, titleValue);

        url = jenkinsBaseUrl + "buildWithParameters?" + refId +title;
        return url;
    }

    private String getBaseUrl() {
        String baseUrl = jenkinsBaseUrl.toUpperCase().startsWith("HTTP") ? jenkinsBaseUrl : "http://" + jenkinsBaseUrl;
        baseUrl = jenkinsBaseUrl.lastIndexOf('/') == jenkinsBaseUrl.length() ? jenkinsBaseUrl : jenkinsBaseUrl + "/";
        return baseUrl;
    }

    /**
     * Add a general comment to the pull-request with information about the commit id and link to the job
     * @param pushEvent
     * @param response 
     * @param jobNumber - Jenkins Job number
     */
    private void addComment(PullRequestEvent pushEvent, HttpResponse response, String jobNumber) {
        Header headers = response.getFirstHeader("Location");
        String responseUrl = jenkinsBaseUrl;
        if (headers != null && jobNumber != null) {
            responseUrl = headers.getValue()+jobNumber+"/";
        }
        String eventType = PULLREQUEST_EVENT_CREATED;
        if (pushEvent instanceof PullRequestRescopedEvent) {
           eventType = PULLREQUEST_EVENT_SOURCE_UPDATED;
        } else if (pushEvent instanceof PullRequestReopenedEvent) {
           eventType = PULLREQUEST_EVENT_REOPEN;
        }
        String comment = String.format("Build triggered\nEvent: %s\nCommit id: %s\nJob: %s",eventType,pushEvent.getPullRequest().getFromRef().getLatestChangeset(),responseUrl);
        pullRequestService.addComment(pushEvent.getPullRequest().getToRef().getRepository().getId(), pushEvent.getPullRequest().getId(), comment);
    }

    /**
     * Parse the JSON output and retrieve the next build number
     * @param json - json output from Jenkins
     * @return - the next build number
     */
    private String nextBuildNo(String json) {
        int startIdx = json.indexOf(NEXT_BUILD_NUMBER)+NEXT_BUILD_NUMBER.length();
        return json.substring(startIdx+2, json.indexOf(',', startIdx));
    }

    private HttpResponse httpClientRequest(HttpRequestBase request, String userName, String password) throws IOException, ClientProtocolException {
        DefaultHttpClient client;
        client = new DefaultHttpClient();

        BasicHttpContext context = new BasicHttpContext();

        if (userName != null && password != null) {
            client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), new UsernamePasswordCredentials(userName, password));

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
        loadPluginSettings();
        if (triggerBuildOnCreate && validateSettings()) {
            triggerBuild(pushEvent);
        }
    }
    
    @EventListener
    public void updatePullRequest(PullRequestRescopedEvent pushEvent)
    {
        loadPluginSettings();
        boolean isSourceChanged = !pushEvent.getPullRequest().getFromRef().getLatestChangeset().equals(pushEvent.getPreviousFromHash());
        
        if ((triggerBuildOnUpdate) && (!isAutomaticBuildDisabled(pushEvent)) && (validateSettings()) && (isSourceChanged)) {
            triggerBuild(pushEvent);
        }
    }
    
    @EventListener
    public void reopenPullRequest(PullRequestReopenedEvent pushEvent)
    {
        loadPluginSettings();
        if (triggerBuildOnReopen && !isAutomaticBuildDisabled(pushEvent)) {
            triggerBuild(pushEvent);
         
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
