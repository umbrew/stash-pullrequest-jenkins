(function($) {
    // Set up our namespace
    window.PULLREQUEST = window.PULLREQUEST || {};
    PULLREQUEST = PULLREQUEST || {};

    function storageKey(pullRequestJson) {
        var repo = pullRequestJson.toRef.repository;
        var proj = repo.project;
        return 'stash.plugin.jenkins.settingsui.' + proj.key + '/' + repo.slug + '/' + pullRequestJson.id;
    }
    
    function getSettingsServletUrl(pullRequestJson) {
		var baseUrl = AJS.contextPath();
    	var repo = pullRequestJson.toRef.repository;
        var proj = repo.project;
        return baseUrl+'/plugins/servlet/jenkins/settings/' + proj.key + '/' + repo.slug;
    }
    
    function getManualTriggerServletUrl(pullRequestJson) {
		var baseUrl = AJS.contextPath();
    	var repo = pullRequestJson.toRef.repository;
        return baseUrl+'/plugins/servlet/jenkins/manualtrigger/' + repo.id + '/' + repo.slug;
    }
    
    var storage = { getCheckBoxStatus : function(pullRequestJson) {
             AJS.$.ajax({
	          type: "GET",
	          url: getSettingsServletUrl(pullRequestJson)+'/'+pullRequestJson.id,
	          success: function(data) {
							var checkBox = $('#disablecheckbox_id');
					        if (data != null && data.length > 0) {
					        	checkBox.attr('checked', true);
					        } else {
					        	checkBox.attr('checked', false);
					        };
	                   }
	          });
        },
        addCheckBoxStatus : function(pullRequestJson, disableBuild) {
	         AJS.$.ajax({
	          type: "POST",
	          url: getSettingsServletUrl(pullRequestJson)+'/'+pullRequestJson.id,
	          data: {disableBuildParameter : disableBuild}
	          });
        },
        triggerBuild : function(pullRequestJson) {
	         AJS.$.ajax({
	          type: "POST",
	          url: getManualTriggerServletUrl(pullRequestJson)+'/'+pullRequestJson.id});
       }
    };

    // Stash 2.4.x and 2.5.x incorrectly provided a Brace/Backbone model here, but should have provided raw JSON.
    function coerceToJson(pullRequestOrJson) {
        return pullRequestOrJson.toJSON ? pullRequestOrJson.toJSON() : pullRequestOrJson;
    }

    function setCheckboxStatus(pullRequestJson, disableBuild) {
        storage.addCheckBoxStatus(pullRequestJson, disableBuild);
    }
    
    function triggerBuild(pullRequestJson) {
        storage.triggerBuild(pullRequestJson);
    }
    
	function setDisableAutomaticCheckboxOnLoad() {
	   var pr = require('model/page-state').getPullRequest();
	   storage.getCheckBoxStatus(coerceToJson(pr));
       
    }

    PULLREQUEST.setCheckboxStatus = setCheckboxStatus;
    PULLREQUEST.setDisableAutomaticCheckboxOnLoad = setDisableAutomaticCheckboxOnLoad;
    PULLREQUEST.triggerBuild = triggerBuild;
    
    /* Expose the client-condition function */
    PULLREQUEST._pullRequestIsOpen = function(context) {
        var pr = coerceToJson(context['pullRequest']);
        return pr.state === 'OPEN';
    };

	$(document).ready(function(){
	    var pr = require('model/page-state').getPullRequest();
	    if (coerceToJson(pr).state == 'OPEN') {
	       PULLREQUEST.setDisableAutomaticCheckboxOnLoad();
	    }
	    
	     $("#disablecheckbox_id").unbind("click").click(function(e) {
			if($('#disablecheckbox_id').is(":checked")) {
			  PULLREQUEST.setCheckboxStatus(coerceToJson(pr), "checked");
			} else {
			  PULLREQUEST.setCheckboxStatus(coerceToJson(pr), "");
			}
			return;
	    });
	     
	     $("#triggerbuild_id").unbind("click").click(function(e) {
			PULLREQUEST.triggerBuild(coerceToJson(pr));
			return;
		  });
    });
   
}(AJS.$));