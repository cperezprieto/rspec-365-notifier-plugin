package org.jenkinsci.plugins.microsoftnotifier.workflow;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.jenkinsci.plugins.microsoftnotifier.RspecMicrosoft;
import org.jenkinsci.plugins.microsoftnotifier.RspecMicrosoftService;
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

public class RspecMicrosoftStep extends AbstractStepImpl {

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
    public RspecMicrosoftStep(String jobWebhook) {
        this.jobWebhook = jobWebhook;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(RspecMicrosoftSendExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "rspecMicrosoftSend";
        }

        @Override
        public String getDisplayName() {
            return "Send rspec notifications to 365";
        }
    }

    public static class RspecMicrosoftSendExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        @Inject
        private transient RspecMicrosoftStep step;

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
            
            RspecMicrosoft.RspecMicrosoftDescriptor rspecMicrosoftDesc = jenkins.getDescriptorByType(RspecMicrosoft.RspecMicrosoftDescriptor.class);
            
            String webHookEndpoint = rspecMicrosoftDesc.getWebHookEndpoint();
            String json = step.json;
            boolean hideSuccessfulResults = step.hideSuccessfulResults;
            String jobWebhook = step.jobWebhook;
            String extra = step.extra;
            
            if(jobWebhook != null && jobWebhook != "")
            	webHookEndpoint = jobWebhook;
            
            if(webHookEndpoint == null || webHookEndpoint == "")
            	throw new AbortException("Unable to send report to 365. Webhook not configured.");

            RspecMicrosoftService microsoftService = new RspecMicrosoftService(webHookEndpoint);
            
            try {
            	microsoftService.sendRspecReportToMicrosoft(run, workspace, json, extra, hideSuccessfulResults);
            } catch (Exception exp) {
            	if (step.failOnError) {
            		throw new AbortException("Unable to send 365 notification: " + exp);
            	}
            }
            
            return null;
        }

        /**
         * Helper method for logging.
         */
        private void log(String format, Object... args) {
            this.listener.getLogger().println("[Rspec365Notifier] " + String.format(format, args));
        }
    }
}
