package com.harms.stash.plugin.jenkins.job.intergration;

/**
 * Define the different types of a events for a Jenkins job.
 * @author fharms
 *
 */
public enum TriggerRequestEvent {

    PULLREQUEST_EVENT_CREATED("CREATED"),
    PULLREQUEST_EVENT_SOURCE_UPDATED("SOURCE UPDATED"),
    PULLREQUEST_EVENT_REOPEN("REOPEN"),
    FORCED_BUILD("FORCED A MANUAL BUILD");
    
    private final String eventText;
    
    private TriggerRequestEvent(String eventText) {
        this.eventText = eventText;
    }
    
    public String getText() {
        return this.eventText;
    }
}
