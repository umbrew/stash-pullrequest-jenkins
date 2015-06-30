package com.harms.stash.plugin.jenkins.job.intergration;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.scheduler.SchedulerService;
import com.atlassian.stash.pull.PullRequestService;
import com.atlassian.stash.user.SecurityService;
import com.atlassian.stash.user.UserService;

/**
 * Register/unegister the Job Runner when the bean is Initializing / Disposable
 * 
 * @author fharms
 *
 */
public class JenkinsJobSchedulerRegister implements DisposableBean, InitializingBean { 

    private final SchedulerService schedulerService;
	private final PullRequestService pullRequestService;
	private final UserService userService;
	private final SecurityService securityService;
	private final JobTrigger jenkinsCiIntergration; 

    public JenkinsJobSchedulerRegister(SchedulerService schedulerService, PullRequestService pullRequestService,UserService userService,SecurityService securityService,JobTrigger jenkinsCiIntergration) { 
        this.schedulerService = schedulerService;
		this.pullRequestService = pullRequestService;
		this.userService = userService;
		this.securityService = securityService;
		this.jenkinsCiIntergration = jenkinsCiIntergration; 
    } 

    @Override 
    public void afterPropertiesSet()  { 
    	schedulerService.registerJobRunner(JenkinsJobScheduler.jobRunnerKey, new JenkinsJobScheduler(pullRequestService,userService,securityService,jenkinsCiIntergration));
    	    
    } 

    @Override 
    public void destroy() { 
        schedulerService.unregisterJobRunner(JenkinsJobScheduler.jobRunnerKey); 
    } 
}