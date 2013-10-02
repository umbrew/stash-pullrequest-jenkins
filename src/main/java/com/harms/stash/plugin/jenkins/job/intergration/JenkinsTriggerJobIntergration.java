package com.harms.stash.plugin.jenkins.job.intergration;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
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
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
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
import com.atlassian.stash.event.pull.PullRequestOpenedEvent;
import com.atlassian.stash.event.pull.PullRequestReopenedEvent;
import com.atlassian.stash.event.pull.PullRequestRescopedEvent;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestService;

public class JenkinsTriggerJobIntergration {

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
	
//	@Override
//	public void postReceive(RepositoryHookContext context,Collection<RefChange> refChanges) {
//	      getPluginSettings(context.getSettings());
//	      
//    	  for (RefChange change : refChanges) {
//              if (change.getType() == RefChangeType.UPDATE || change.getType() == RefChangeType.ADD) {
//            	  Page<PullRequest> page = pullRequestService.findInDirection(PullRequestDirection.OUTGOING,
//                          context.getRepository().getId(), change.getRefId(), PullRequestState.OPEN, null,
//                          PageUtils.newRequest(0, 1));
//            	  if (page != null) {
//            		  String hash = change.getToHash();
//            		  if (page.getSize() > 0) {
//            		      Iterator<PullRequest> pullRequests = page.getValues().iterator();
//            		      PullRequest pr = ((PullRequest)pullRequests);
//            		      triggerBuild(hash,pr.getTitle());
//            		      String comment = String.format("Build triggered for %s on %s",hash,jenkinsBaseUrl);
//            		      pullRequestService.addComment(context.getRepository().getId(), pr.getId(), comment);
//            		  }
//            	  }
//              }
//          }
//	}
	
    private void triggerBuild(String refId, String title) {

        String url = "";
        DefaultHttpClient client = null;
        HttpResponse response = null;
        try {
            refId = String.format("%s=%s", buildRefField, URLEncoder.encode(refId, "utf-8"));
            title = buildTitleField == null || buildTitleField.isEmpty() ? "" : String.format("&%s=%s", buildTitleField, URLEncoder.encode(title, "utf-8"));

            url = jenkinsBaseUrl.toUpperCase().startsWith("HTTP") ? jenkinsBaseUrl : "http://" + jenkinsBaseUrl;
            url = jenkinsBaseUrl.lastIndexOf('/') == jenkinsBaseUrl.length() ? jenkinsBaseUrl : jenkinsBaseUrl + "/";
            url = jenkinsBaseUrl + "?" + refId + title;

            client = new DefaultHttpClient();

            client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), new UsernamePasswordCredentials(userName, password));

            BasicScheme basicAuth = new BasicScheme();
            BasicHttpContext context = new BasicHttpContext();
            context.setAttribute("preemptive-auth", basicAuth);

            client.addRequestInterceptor((HttpRequestInterceptor) new PreemptiveAuth(), 0);

            HttpPost get = new HttpPost(url);

            response = client.execute(get, context);
            HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (response.getStatusLine().getStatusCode() >= 400) {
                RuntimeException e = new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
                log.error("Error triggering: " + url, e);
                throw e;
            }
        }
    }
    
    @EventListener
    public void openPullRequest(PullRequestOpenedEvent pushEvent)
    {
        getPluginSettings();
        if (triggerBuildOnCreate) {
            PullRequest pr = pushEvent.getPullRequest();
            String latestChangeset = pr.getFromRef().getLatestChangeset();
            triggerBuild(latestChangeset,pr.getTitle());
            String comment = String.format("Build triggered for %s on %s",latestChangeset,jenkinsBaseUrl);
            pullRequestService.addComment(pr.getFromRef().getRepository().getId(), pr.getId(), comment);
        }
    }
    
    @EventListener
    public void updatePullRequest(PullRequestRescopedEvent pushEvent)
    {
        getPluginSettings();
        if (triggerBuildOnUpdate) {
            PullRequest pr = pushEvent.getPullRequest();
            String latestChangeset = pr.getFromRef().getLatestChangeset();
            triggerBuild(latestChangeset,pr.getTitle());
            String comment = String.format("Build triggered for %s on %s",latestChangeset,jenkinsBaseUrl);
            pullRequestService.addComment(pr.getFromRef().getRepository().getId(), pr.getId(), comment);
        }
    }
    
    @EventListener
    public void reopenPullRequest(PullRequestReopenedEvent pushEvent)
    {
        getPluginSettings();
        if (triggerBuildOnReopen) {
            PullRequest pr = pushEvent.getPullRequest();
            String latestChangeset = pr.getFromRef().getLatestChangeset();
            triggerBuild(latestChangeset,pr.getTitle());
            String comment = String.format("Build triggered for %s on %s",latestChangeset,jenkinsBaseUrl);
            pullRequestService.addComment(pr.getFromRef().getRepository().getId(), pr.getId(), comment);
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
