package com.harms.stash.plugin.jenkins.job.settings.servlet;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestService;
import com.harms.stash.plugin.jenkins.job.intergration.JobTrigger;
import com.harms.stash.plugin.jenkins.job.intergration.PullRequestData;
import com.harms.stash.plugin.jenkins.job.intergration.TriggerRequestEvent;

public class ManualTriggerBuild extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(ManualTriggerBuild.class);
    
    private static final long serialVersionUID = -6947257382708409328L;
    private final JobTrigger jenkinsCiIntergration;
    private PullRequestService pullRequestService;
    
    public ManualTriggerBuild(JobTrigger jenkinsCiIntergration, PullRequestService pullRequestService) {
        log.debug("invoked constructor");
        this.pullRequestService = pullRequestService;
        this.jenkinsCiIntergration = jenkinsCiIntergration;
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        log.debug(String.format("invoked with path info %s", pathInfo));

        String[] components = pathInfo.split("/");

        log.debug(String.format("serlvet parameters %s", Arrays.toString(components)));
        
        if (components.length != 4) {
            log.error(String.format("Servlet ManualTriggerBuild not invoked with the correct number of parameters %s", Arrays.toString(components)));
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        Long pullRequestId = new Long(components[3]);
        Integer repositoryId = new Integer(components[1]);
        
        PullRequest pullRequest = pullRequestService.getById(repositoryId, pullRequestId);
        if (pullRequest != null) {
            log.debug(String.format("Retrieved pull request information for %s", pullRequest.getId()));
            PullRequestData prd = new PullRequestData(pullRequest);
            String baseUrl = jenkinsCiIntergration.nextCIServer(prd.slug);
            log.debug(String.format("Retrieved the build server url %s", baseUrl));
            if (baseUrl != null) {
                log.debug(String.format("Trigger build for repository %s, commit id %s, pull request %s, title %s, slug %s", prd.repositoryId,prd.latestChanges,prd.pullRequestId, pullRequest.getTitle(), prd.slug));
                jenkinsCiIntergration.triggerBuild(prd.repositoryId, 
                        prd.latestChanges, 
                        pullRequestId, 
                        pullRequest.getTitle(), 
                        prd.slug, 
                        TriggerRequestEvent.FORCED_BUILD, 0, baseUrl);
                resp.setStatus(HttpServletResponse.SC_OK);
            }else {
                log.error(String.format("Not able to retrieve Jenkins base url based on the slug id %s",prd.slug));
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Not able to retrieve Jenkins base url based on the slug id %s",prd.slug));  
            }
        } else {
            log.error(String.format("Not able to retrieve the pull-reqeust based on the repository id %s and pull-request id %s",repositoryId,pullRequestId));
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Not able to retrieve the pull-reqeust based on the repository id %s and pull-request id %s",repositoryId,pullRequestId));
        }
    }
}
