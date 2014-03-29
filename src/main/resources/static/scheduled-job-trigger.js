var pageScheduleInitialized = false;
(function($) {
    // Set up our namespace
    window.JOBTRIGGER = window.JOBTRIGGER || {};
    JOBTRIGGER = JOBTRIGGER || {};

    function getScheduleJobTriggerServletUrl(pullRequestJson) {
		var baseUrl = AJS.contextPath();
    	var repo = pullRequestJson.toRef.repository;
        var proj = repo.project;
        return baseUrl+'/plugins/servlet/jenkins/scheduledtriggers/' + repo.slug;
    }
    
    var storage = { getCheckBoxStatus : function(pullRequestJson) {
             AJS.$.ajax({
	          type: "GET",
	          url: getScheduleJobTriggerServletUrl(pullRequestJson)+'/'+pullRequestJson.id,
	          success: function(data) {
					        if (data != null && data.length > 0) {
					        	AJS.messages.generic("#schedule-job-trigger-message",{
					        		   title:"Jenkins Job is scheduled to trigger at "+data
					        		});
					        };
	                   }
	          });
        }
    };

    // Stash 2.4.x and 2.5.x incorrectly provided a Brace/Backbone model here, but should have provided raw JSON.
    function coerceToJson(pullRequestOrJson) {
        return pullRequestOrJson.toJSON ? pullRequestOrJson.toJSON() : pullRequestOrJson;
    }

	function getScheduledJobTriggerOnLoad() {
	   var pr = require('model/page-state').getPullRequest();
	   storage.getCheckBoxStatus(coerceToJson(pr));
       
    }

    JOBTRIGGER.getScheduledJobTriggerOnLoad = getScheduledJobTriggerOnLoad;
    
    /* Expose the client-condition function */
    JOBTRIGGER._pullRequestIsOpen = function(context) {
        var pr = coerceToJson(context['pullRequest']);
        return pr.state === 'OPEN';
    };

	$(document).ready(function(){
		if(pageScheduleInitialized) return;
		pageScheduleInitialized = true;
	    var pr = require('model/page-state').getPullRequest();
	    if (coerceToJson(pr).state == 'OPEN') {
	       JOBTRIGGER.getScheduledJobTriggerOnLoad();
	    }
    });
   
}(AJS.$));