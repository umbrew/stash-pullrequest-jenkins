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

import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.stash.pull.PullRequestService;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;

final public class JenkinsJobTrigger implements JobTrigger {
    private static final Logger log = LoggerFactory.getLogger(JenkinsJobTrigger.class);

    private final PullRequestService pullRequestService;
    private final PluginSettings settings;
    private final WebResourceUrlProvider webResourceUrlProvider;

    private volatile String[] serverList;


	public JenkinsJobTrigger(PullRequestService pullRequestService, PluginSettingsFactory pluginSettingsFactory, WebResourceUrlProvider webResourceUrlProvider) {
		this.pullRequestService = pullRequestService;
        this.webResourceUrlProvider = webResourceUrlProvider;
        this.settings = pluginSettingsFactory.createGlobalSettings();
	}

	/* (non-Javadoc)
     * @see com.harms.stash.plugin.jenkins.job.intergration.TriggerJobIntergration#nextCIServer(java.lang.String)
     */
	@Override
    public String nextCIServer(String slug) {
	    String lastCiServer = null;
	    synchronized (settings) {

	        serverList = PluginSettingsHelper.getJenkinsCIServerList(slug, settings);

	        lastCiServer = serverList[0];

	        String lastStoredCiServer = PluginSettingsHelper.getLastJenkinsCIServer(slug, settings);
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
    	    PluginSettingsHelper.setLastJenkinsCIServer(slug, lastCiServer, settings);
	    }
	    return lastCiServer;
	}

	/* (non-Javadoc)
     * @see com.harms.stash.plugin.jenkins.job.intergration.TriggerJobIntergration#validateSettings(java.lang.String, java.lang.String)
     */
	@Override
    public boolean validateSettings(String jenkinsBaseUrl, String slug) {
	    return (jenkinsBaseUrl != null) && (PluginSettingsHelper.getBuildReferenceField(slug, settings) != null);
	}

    /* (non-Javadoc)
     * @see com.harms.stash.plugin.jenkins.job.intergration.TriggerJobIntergration#triggerBuild(java.lang.Integer, java.lang.String, java.lang.Long, java.lang.String, java.lang.String, com.harms.stash.plugin.jenkins.job.intergration.TriggerRequestEvent, int, java.lang.String)
     */
    @Override
    public void triggerBuild(Integer toRefRepositoryId, String latestChangeset, Long pullRequestId, String pullRequestTitle, String slug, TriggerRequestEvent eventType, int retryCount, String baseUrl, String projectKey, String fromBranchId, String toBranchId) {
        String url = "";
        HttpResponse response = null;

        try {
            url = buildJenkinsUrl(latestChangeset, pullRequestId,pullRequestTitle,slug, baseUrl, projectKey, fromBranchId, toBranchId);
            HttpPost post = new HttpPost(url);
            byte[] userName = PluginSettingsHelper.getUsername(slug, settings);
            byte[] password = PluginSettingsHelper.getPassword(slug, settings);
            response = httpClientRequest(post, userName, password);
            EntityUtils.consume(response.getEntity());
        } catch (Exception e) {
            if (!retryTriggerJob(toRefRepositoryId,latestChangeset,pullRequestId,pullRequestTitle,slug,projectKey,eventType, retryCount, url,-1,e.getMessage(), fromBranchId, toBranchId)) {
                addErrorComment(toRefRepositoryId, pullRequestId, String.format("Failed to trigger build %s\nException : %s",url,e.getMessage()));
                throw new RuntimeException(e);
            }
        } finally {
            if (response != null) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 400) {
                    if (!retryTriggerJob(toRefRepositoryId,latestChangeset,pullRequestId,pullRequestTitle,slug,projectKey,eventType, retryCount, url, statusCode,"", fromBranchId, toBranchId)) {
                        addErrorComment(toRefRepositoryId, pullRequestId, String.format("All CI servers failed, no job is triggered",url));
                        throw new RuntimeException("All CI servers failed, no job is triggered\nFailed : HTTP error code : " + statusCode);
                    }
                } else {

                    addComment(toRefRepositoryId,pullRequestId,
                            eventType,
                            latestChangeset,
                            response, baseUrl);
                }
            }
        }
    }

    private boolean retryTriggerJob(Integer toRefRepositoryId, String latestChangeset, Long pullRequestId, String pullRequestTitle, String slug, String projectId, TriggerRequestEvent eventType, int retryCount, String url, int status, String errorText, String fromBranchId, String toBranchId)  {
        String baseUrl;
        //try the next server in case of an error
        if (retryCount++ < serverList.length-1) {
            String comment = String.format("Try next CI server in the list, failed to call %s(%s)\n%s",url,status,errorText);
            addErrorComment(toRefRepositoryId, pullRequestId, comment);
            baseUrl = nextCIServer(slug);
            triggerBuild(toRefRepositoryId,latestChangeset, pullRequestId, pullRequestTitle, slug, eventType, retryCount, baseUrl, projectId, fromBranchId, toBranchId);
            return true;
        }
        return false;
    }

    /**
     * Build up the URL for trigger job on Jenkins with the specified parameters
     * @param latestChanges - SHA commit id
     * @param pullRequestId - The id of the pull request
     * @param pullRequestTitle - The title of the pull request
     * @param slug
     * @param fromBranch - From Branch name, the origin of the pull request
     * @param toBranch - To Branch name, the destination of the pull request
     * @return A correct formatted URL for trigger a Jenkins job
     * @throws UnsupportedEncodingException
     */
    private String buildJenkinsUrl(String latestChanges,Long pullRequestId, String pullRequestTitle, String slug, String jenkinsBaseUrl, String projectKey, String fromBranchId, String toBranchId) throws UnsupportedEncodingException {
        String url;
        String buildRefField = PluginSettingsHelper.getBuildReferenceField(slug, settings);
        String refId = String.format("%s=%s", buildRefField, URLEncoder.encode(latestChanges, "utf-8"));
        @SuppressWarnings("deprecation")
        String titleValue = URLEncoder.encode(String.format("pull-request #%s - %s", pullRequestId,pullRequestTitle, "utf-8"));
        String buildTitleField = PluginSettingsHelper.getBuildTitleField(slug, settings);
        String title = buildTitleField == null || buildTitleField.isEmpty() ? "" : String.format("&%s=%s", buildTitleField, titleValue);

        String pullRequestUrlValue = String.format("%s/projects/%s/repos/%s/pull-requests/%s",webResourceUrlProvider.getBaseUrl(UrlMode.ABSOLUTE),projectKey,slug,pullRequestId);
        String buildPullRequestUrlField = PluginSettingsHelper.getPullRequestUrlFieldName(slug, settings);
        String pullRequestUrl = buildPullRequestUrlField == null || buildPullRequestUrlField.isEmpty() ? "" : String.format("&%s=%s", buildPullRequestUrlField, pullRequestUrlValue);

        String fromBranchUrl = fromBranchId == null || fromBranchId.isEmpty() ? "" : String.format("&%s=%s", "fromBranch", fromBranchId);
        String toBranchUrl = toBranchId == null || toBranchId.isEmpty() ? "" : String.format("&%s=%s", "toBranch", toBranchId);

        url = jenkinsBaseUrl + "buildWithParameters?" + refId + title + pullRequestUrl + fromBranchUrl + toBranchUrl;
        return url.trim();
    }

    private String getBaseUrl(String jenkinsBaseUrl) {
        String baseUrl = jenkinsBaseUrl.toUpperCase().startsWith("HTTP") ? jenkinsBaseUrl : "http://" + jenkinsBaseUrl;
        baseUrl = jenkinsBaseUrl.lastIndexOf('/') == jenkinsBaseUrl.length()-1 ? jenkinsBaseUrl : jenkinsBaseUrl + "/";
        return baseUrl;
    }

    /**
     * Add a general comment to the pull-request with information about the commit id and link to the job
     * @param repositoryId - The id of the current repository
     * @param pullRequestId - The id of the current pull-request
     * @param eventType - The type of the event
     * @param response
     */
    private void addComment(Integer repositoryId, Long pullRequestId,TriggerRequestEvent eventType, String lastChangeSet, HttpResponse response, String jenkinsBaseUrl) {
        String comment = String.format("Build triggered\nEvent: %s\nCommit id: %s\nJob: %s",eventType.getText(),lastChangeSet,jenkinsBaseUrl);
        pullRequestService.addComment(repositoryId, pullRequestId, comment);
    }

    /**
     * Add a error message to the pull-request comment section
     * @param repositoryId - The id of the current repository
     * @param pullRequestId - The id of the current pull-request
     * @param e - the {@link Exception}
     */
    private void addErrorComment(Integer repositoryId, Long pullRequestId, String comment) {
          pullRequestService.addComment(repositoryId,pullRequestId, comment);
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
