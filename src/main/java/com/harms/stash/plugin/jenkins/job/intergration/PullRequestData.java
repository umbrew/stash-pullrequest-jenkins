package com.harms.stash.plugin.jenkins.job.intergration;

import com.atlassian.stash.pull.PullRequest;

/**
 * Place holder for storing relevant information used for trigger builds, update pull-request
 * or add comments.
 * 
 * @author fharms
 *
 */
final public class PullRequestData {
    final public String projectKey;
    final public Integer repositoryId;
    final public Long pullRequestId;
    final public String latestChanges;
    final public String title;
    final public String slug;

    public PullRequestData(PullRequest pr) {
        projectKey = pr.getToRef().getRepository().getProject().getKey();
        repositoryId = pr.getToRef().getRepository().getId();
        pullRequestId = pr.getId();
        latestChanges = pr.getFromRef().getLatestChangeset();
        title = pr.getTitle();
        slug = pr.getFromRef().getRepository().getSlug();
    }
}