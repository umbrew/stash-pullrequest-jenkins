package com.harms.stash.plugin.jenkins.job.intergration;

import java.io.IOException;
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
import com.atlassian.stash.event.pull.PullRequestEvent;
import com.atlassian.stash.event.pull.PullRequestOpenedEvent;
import com.atlassian.stash.event.pull.PullRequestReopenedEvent;
import com.atlassian.stash.event.pull.PullRequestRescopedEvent;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestService;

public class JenkinsTriggerJobIntergration {

    private static final String NEXT_BUILD_NUMBER = "nextBuildNumber";

    private static final Logger log = LoggerFactory.getLogger(JenkinsTriggerJobIntergration.class);
    
    private static final String PLUGIN_STORAGE_KEY = "stash.plugin.jenkins.settingsui";
    private static final String BUILD_TITLE_FIELD = PLUGIN_STORAGE_KEY + ".buildTitleField";
    private static final String BUILD_REF_FIELD = PLUGIN_STORAGE_KEY + ".buildRefField";
    private static final String JENKINS_PASSWORD = PLUGIN_STORAGE_KEY + ".jenkinsPassword";
    private static final String JENKINS_USERNAME = PLUGIN_STORAGE_KEY + ".jenkinsUserName";
    private static final String JENKINS_BASE_URL = PLUGIN_STORAGE_KEY + ".jenkinsBaseUrl";
    private static final String TRIGGER_BUILD_ON_CREATE = PLUGIN_STORAGE_KEY + ".triggerBuildOnCreate";
    private static final String TRIGGER_BUILD_ON_UPDATE = PLUGIN_STORAGE_KEY + ".triggerBuildOnUpdate";
    private static final String TRIGGER_BUILD_ON_REOPEN = PLUGIN_STORAGE_KEY + ".triggerBuildOnReopen";
 
    
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
	
	private void getPluginSettings() {
	    PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
	    userName = "";
	    password = "";
	    buildTitleField = "";
	    
        if (pluginSettings.get(JENKINS_BASE_URL) != null){
            jenkinsBaseUrl = (String) pluginSettings.get(JENKINS_BASE_URL);
        }
        
        if (pluginSettings.get(JENKINS_USERNAME) != null){
            userName = (String) pluginSettings.get(JENKINS_USERNAME);
        }
        
        if (pluginSettings.get(JENKINS_PASSWORD) != null){
            password = (String) pluginSettings.get(JENKINS_PASSWORD);
        }
        
        if (pluginSettings.get(BUILD_REF_FIELD) != null){
            buildRefField = (String) pluginSettings.get(BUILD_REF_FIELD);
        }
        
        if (pluginSettings.get(BUILD_TITLE_FIELD) != null){
           buildTitleField = (String) pluginSettings.get(BUILD_TITLE_FIELD);
        }
        
        if (pluginSettings.get(TRIGGER_BUILD_ON_CREATE) != null){
            triggerBuildOnCreate = true;
        }
        
        if (pluginSettings.get(TRIGGER_BUILD_ON_REOPEN) != null){
            triggerBuildOnReopen = true;
        }
        
        if (pluginSettings.get(TRIGGER_BUILD_ON_UPDATE) != null){
            triggerBuildOnUpdate = true;
        }
	}
	
    private void triggerBuild(PullRequestEvent pushEvent) {
        String url = "";
        HttpResponse response = null;
        String nextBuildNo = null;

        PullRequest pr = pushEvent.getPullRequest();
        try {
            String refId = String.format("%s=%s", buildRefField, URLEncoder.encode(pr.getFromRef().getLatestChangeset(), "utf-8"));
            String title = buildTitleField == null || buildTitleField.isEmpty() ? "" : String.format("&%s=%s", buildTitleField, URLEncoder.encode(pr.getTitle(), "utf-8"));

            String baseUrl = jenkinsBaseUrl.toUpperCase().startsWith("HTTP") ? jenkinsBaseUrl : "http://" + jenkinsBaseUrl;
            baseUrl = jenkinsBaseUrl.lastIndexOf('/') == jenkinsBaseUrl.length() ? jenkinsBaseUrl : jenkinsBaseUrl + "/";
            url = jenkinsBaseUrl + "buildWithParameters?" + refId + title;
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
                Header headers = response.getFirstHeader("Location");
                String responseUrl = jenkinsBaseUrl;
                if (headers != null && nextBuildNo != null) {
                    responseUrl = headers.getValue()+nextBuildNo+"/";
                }
                String comment = String.format("Build triggered for %s on %s",pr.getFromRef().getLatestChangeset(),responseUrl);
                pullRequestService.addComment(pr.getFromRef().getRepository().getId(), pr.getId(), comment);
            }
        }
    }

    private String nextBuildNo(String json) {
        int startIdx = json.indexOf(NEXT_BUILD_NUMBER)+NEXT_BUILD_NUMBER.length();
        return json.substring(startIdx+2, json.indexOf(',', startIdx));
    }

    private HttpResponse httpClientRequest(HttpRequestBase request, String userName, String password) throws IOException, ClientProtocolException {
        DefaultHttpClient client;
        client = new DefaultHttpClient();

        client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), new UsernamePasswordCredentials(userName, password));

        BasicScheme basicAuth = new BasicScheme();
        BasicHttpContext context = new BasicHttpContext();
        context.setAttribute("preemptive-auth", basicAuth);

        client.addRequestInterceptor((HttpRequestInterceptor) new PreemptiveAuth(), 0);

        HttpResponse response = client.execute(request, context);
        return response;
    }
    
    @EventListener
    public void openPullRequest(PullRequestOpenedEvent pushEvent)
    {
        getPluginSettings();
        if (triggerBuildOnCreate) {
            triggerBuild(pushEvent);
        }
    }
    
    @EventListener
    public void updatePullRequest(PullRequestRescopedEvent pushEvent)
    {
        getPluginSettings();
        if (triggerBuildOnUpdate) {
            triggerBuild(pushEvent);
        }
    }
    
    @EventListener
    public void reopenPullRequest(PullRequestReopenedEvent pushEvent)
    {
        getPluginSettings();
        if (triggerBuildOnReopen) {
            triggerBuild(pushEvent);
         
        }
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
