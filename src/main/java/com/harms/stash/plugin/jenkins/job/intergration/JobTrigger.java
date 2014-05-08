package com.harms.stash.plugin.jenkins.job.intergration;


/**
 * 
 * @author fharms
 *
 */
public interface JobTrigger {

    /**
     * @return the next CI server from the list of servers
     */
    public String nextCIServer(String slug);

    /**
     * Validate the it got the correct settings before it trigger a build
     * @param jenkinsBaseUrl
     * @param slug
     * @return
     */
    public boolean validateSettings(String jenkinsBaseUrl, String slug);

    /**
     * @param toRefRepositoryId - Id to the repository
     * @param latestChangeset - The SHA commit id
     * @param pullRequestId - The current pull-request id
     * @param pullRequestTitle - The title of the pull-request
     * @param slug - The slug id
     * @param eventType - The type of job trigger event
     * @param retryCount - Number of current retries
     * @param baseUrl - the Jenkins CI base URL
     * @param projectId - The project id
     */
    public void triggerBuild(Integer toRefRepositoryId, String latestChangeset, Long pullRequestId, String pullRequestTitle, String slug, TriggerRequestEvent eventType,
            int retryCount, String baseUrl, String projectId);
    

}