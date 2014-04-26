package com.harms.stash.plugin.jenkins.job.intergration;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.scheduling.PluginJob;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import com.atlassian.stash.build.BuildStatus;
import com.atlassian.stash.build.BuildStatus.State;
import com.atlassian.stash.build.BuildStatusService;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestSearchRequest;
import com.atlassian.stash.pull.PullRequestSearchRequest.Builder;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.pull.PullRequestState;
import com.atlassian.stash.util.Page;
import com.atlassian.stash.util.PageRequestImpl;
import com.harms.stash.plugin.jenkins.job.settings.PluginSettingsHelper;

public class PxeScheduler implements PluginJob, PxeBootScheduler {

    private static final String PXE_BOOT_SCHEDULER = "pxeBootScheduler";
    private final long repeatInterval = 1 * 60 * 100; // 1 min
    private final PluginScheduler pluginScheduler;
    private final PullRequestService repositoryService;
    private final PluginSettings pluginSettings;
    private final BuildStatusService buildStatusService;
    
    public PxeScheduler(PluginScheduler pluginScheduler, PullRequestService prService, PluginSettingsFactory psf, BuildStatusService buildStatusService) {
        this.pluginScheduler = pluginScheduler;
        this.repositoryService = prService;
        this.buildStatusService = buildStatusService;
        this.pluginSettings = psf.createGlobalSettings();
        Calendar.getInstance().getTime();
    }
    
    @Override
    public void start(Map<String, Object> parameters) {
        this.pluginScheduler.scheduleJob(PXE_BOOT_SCHEDULER, PxeScheduler.class, parameters, Calendar.getInstance().getTime(), repeatInterval);
    }
    
    @Override
    public void stop() {
        this.pluginScheduler.unscheduleJob(PXE_BOOT_SCHEDULER);
    }

    @Override
    public void execute(Map<String, Object> jobDataMap) {
        Integer repositoryId = (Integer) jobDataMap.get("repositoryId");
        Builder builder = new PullRequestSearchRequest.Builder();
        PullRequestSearchRequest searchRequest = builder.fromRepositoryId(repositoryId).state(PullRequestState.OPEN).build();
        
        PageRequestImpl pageRequest = new PageRequestImpl(0, 30);
        Page<PullRequest> pullRequests = repositoryService.search(searchRequest, pageRequest);
        Iterator<PullRequest> pullRequestsIt = pullRequests.getValues().iterator();
        while (pullRequestsIt.hasNext()) {
            PullRequest pr = (PullRequest) pullRequestsIt.next();
            final String projectId = pr.getFromRef().getRepository().getProject().getKey();
            final String slug = pr.getFromRef().getRepository().getSlug();
            final String pullRequestHostToImage = PluginSettingsHelper.getPullRequestHostToImage(projectId, slug, pr.getId(), pluginSettings);
            if (pullRequestHostToImage != null) {
                //get the build status for the latest build
                String buildUrl = getBuildUrl(pr.getFromRef().getLatestChangeset(), State.SUCCESSFUL);
               if (!buildUrl.isEmpty()) {
                   
               }
            }
            
        }
    }
    
    /**
     * Return the latest build for the specified change set and state 
     * @param csid - Change set to return build url for
     * @param state - State to Filter on
     * @return the URL to the build
     */
    private String getBuildUrl(String csid, State state) {
        Date newestDate = null;
        String buildUrl = "";
        Page<? extends BuildStatus> buildStatusList = buildStatusService.findAll(csid);
        Iterator<? extends BuildStatus> iterator = buildStatusList.getValues().iterator();
        while (iterator.hasNext()) {
            BuildStatus buildStatus = (BuildStatus) iterator.next();
            State buildState = buildStatus.getState();
            Date dateAdded = buildStatus.getDateAdded();
            if (buildState == state && (newestDate == null || dateAdded.getTime() > newestDate.getTime())) {
                newestDate = dateAdded;
                buildUrl = buildStatus.getUrl();
            }
        }
        return buildUrl;
    }
}
