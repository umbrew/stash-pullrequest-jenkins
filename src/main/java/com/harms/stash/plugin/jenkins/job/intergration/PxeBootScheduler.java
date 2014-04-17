package com.harms.stash.plugin.jenkins.job.intergration;

import java.util.Map;

public interface PxeBootScheduler {

    public void start(Map<String, Object> parameters);

    public void stop();

}