package com.harms.stash.plugin.jenkins.job.settings;

import java.util.Map;

/**
 * Interface for for updating and modifying CI settings based on the input from
 * the HTML form.
 * @author fharms
 *
 */
public interface CISupportSettings {

    /**
     * Update the plug-in settings passed on the parameter map from the server
     * @param slug
     * @param parameterMap - A map containing field name and value
     */
    public void updateSettings(String slug, Map<String, String[]> parameterMap) throws Exception;

    /**
     * Read the plug-in settings for the specified slug and return a map containing
     * field name and value
     * @param slug
     * @return - A map containing field name and value for passing to the HTML form
     */
    public Map<String, Object> readSettings(String slug) throws Exception;

}