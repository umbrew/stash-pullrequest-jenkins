package com.harms.stash.plugin.jenkins.job.settings.servlet;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 
 * @author fharms
 *
 */
public class JenkinsIntegrationServlet extends HttpServlet {
    private static final long serialVersionUID = 604820129001885579L;
    private static final Logger log = LoggerFactory.getLogger(JenkinsIntegrationServlet.class);
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String result = new String();
        String pathInfo = req.getPathInfo();
        log.debug(String.format("invoked with path info %s", pathInfo));

        String[] components = pathInfo.split("/");

        log.debug(String.format("serlvet parameters %s", Arrays.toString(components)));
        
        if (components[1].equals("listjobs")) {
            result = retrieveJobList();
        } else {
            log.error(String.format("The JenkinsIntegrationServlet invoked with the incorrect number of parameters %s", Arrays.toString(components)));
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
            
        resp.setContentType("text/plain");
        resp.getWriter().print(result);
        resp.getWriter().flush();
    }

    private String retrieveJobList() {
        return null;
    }

}
