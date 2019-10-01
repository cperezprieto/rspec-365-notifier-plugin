package org.jenkinsci.plugins.microsoftnotifier;

import hudson.Extension;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Job;
import hudson.util.FormValidation;

import java.io.IOException;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class RspecMicrosoft extends JobProperty<Job<?, ?>> {

	@Override
	public RspecMicrosoftDescriptor getDescriptor() {
		return (RspecMicrosoftDescriptor) Jenkins.getInstance().getDescriptor(getClass());
	}

	public static RspecMicrosoftDescriptor get() {
		return (RspecMicrosoftDescriptor) Jenkins.getInstance().getDescriptor(RspecMicrosoft.class);
	}

	@Extension
	public static final class RspecMicrosoftDescriptor extends JobPropertyDescriptor {

		private String webHookEndpoint;
		
		public RspecMicrosoftDescriptor() {
			load();
		}

		@Override
		public String getDisplayName() {
			return "Rsepc 365 Notifier";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			webHookEndpoint = formData.getString("webHookEndpoint");
			save();
			return super.configure(req, formData);
		}

		public String getWebHookEndpoint() {
			return webHookEndpoint;
		}

		public FormValidation doCheckWebHookEndpoint(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() > 0)
			{
				if (value.length() < 20) {
					return FormValidation.warning("Isn't the webHookEndpoint too short?");
				}
	
				if (!value.startsWith("https://outlook.office.com/webhook/")) {
					return FormValidation.warning("365 endpoint should start with https://outlook.office.com/webhook/");
				}
			}

			return FormValidation.ok();
		}
	}
}
