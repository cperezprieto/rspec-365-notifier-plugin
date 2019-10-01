package org.jenkinsci.plugins.microsoftnotifier.workflow;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.jenkinsci.plugins.microsoftnotifier.CucumberMicrosoft;
import org.jenkinsci.plugins.microsoftnotifier.CucumberMicrosoftService;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

public class CucumberMicrosoftStep extends AbstractStepImpl {

    private final String jobWebhook;
    private String json;
    private boolean hideSuccessfulResults;
    private String extra;
    private boolean failOnError;

    @Nonnull
    public String getJobWebhook() {
        return jobWebhook;
    }

    public String getJson() {
        return json;
    }

    public boolean getHideSuccessfulResults() {
        return hideSuccessfulResults;
    }

    public String getExtra() {
        return extra;
    }

    @DataBoundSetter
    public void setJson(String json) {
        this.json = Util.fixEmpty(json);
    }

    @DataBoundSetter
    public void setHideSuccessfulResults(String hideSuccessfulResults) {
        this.hideSuccessfulResults = Boolean.getBoolean(Util.fixEmpty(hideSuccessfulResults));
    }
    
    @DataBoundSetter
    public void setExtra(String extra) {
        this.extra = Util.fixEmpty(extra);
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    @DataBoundSetter
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    @DataBoundConstructor
    public CucumberMicrosoftStep(String jobWebhook) {
        this.jobWebhook = jobWebhook;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CucumberMicrosoftSendExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "cucumberMicrosoftSend";
        }

        @Override
        public String getDisplayName() {
            return "Send cucumber notifications to 365";
        }
    }

    public static class CucumberMicrosoftSendExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        @Inject
        private transient CucumberMicrosoftStep step;

        @StepContextParameter
        private transient TaskListener listener;
        
        @StepContextParameter 
        private transient Run<?,?> run;
        
        @StepContextParameter 
        private transient FilePath workspace;

        @Override
        protected Void run() throws Exception {

            //default to global config values if not set in step, but allow step to override all global settings
            Jenkins jenkins;
            //Jenkins.getInstance() may return null, no message sent in that case
            try {
                jenkins = Jenkins.getInstance();
            } catch (NullPointerException ne) {
                listener.error("Unable to notify 365",ne);
                return null;
            }
            
            CucumberMicrosoft.CucumberMicrosoftDescriptor cucumberMicrosoftDesc = jenkins.getDescriptorByType(CucumberMicrosoft.CucumberMicrosoftDescriptor.class);
            
            String webHookEndpoint = cucumberMicrosoftDesc.getWebHookEndpoint();
            String json = step.json;
            boolean hideSuccessfulResults = step.hideSuccessfulResults;
            String jobWebhook = step.jobWebhook;
            String extra = step.extra;
            
            if(jobWebhook != null && jobWebhook != "")
            	webHookEndpoint = jobWebhook;
            
            if(webHookEndpoint == null || webHookEndpoint == "")
            	throw new AbortException("Unable to send report to 365. Webhook not configured.");

            CucumberMicrosoftService microsoftService = new CucumberMicrosoftService(webHookEndpoint);
            
            try {
            	microsoftService.sendCucumberReportToMicrosoft(run, workspace, json, extra, hideSuccessfulResults);
            } catch (Exception exp) {
            	if (step.failOnError) {
            		throw new AbortException("Unable to send 365 notification: " + exp);
            	}
            }
            
            return null;
        }

    }

}
