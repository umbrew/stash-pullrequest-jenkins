package com.harms.stash.plugin.jenkins.job.settings.upgrade;

import com.atlassian.sal.api.pluginsettings.PluginSettings;

/**
 * The goal for this interface is to provide a guide for implementing upgrade steps between two versions.
 * <pre>
 * void perform(PluginSettings ps) is called when the specific upgrade step is executed. This is the place 
 * where the logic will be implemented for the specific upgrade step
 * 
 * Long getVersion() is called just before the upgrade step is executed, if the upgrade version is
 * higher than the current plug-in version the upgrade step will be executed.
 * </pre> 
 * 
 * @author fharms
 *
 */
public interface Upgrade {
    /**
     * Execute the upgrade step
     * 
     * @param ps - {@link PluginSettings}
     */
    void perform(PluginSettings ps);
    
   
    /**
     * @return the upgrade package version as String
     */
    String getVersion();
}
