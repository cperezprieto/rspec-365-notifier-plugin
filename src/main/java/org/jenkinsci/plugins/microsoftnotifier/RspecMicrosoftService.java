package org.jenkinsci.plugins.microsoftnotifier;

import hudson.FilePath;
import hudson.model.Run;
import jenkins.model.JenkinsLocationConfiguration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;

public class RspecMicrosoftService {

	private static final Logger LOG = Logger.getLogger(RspecMicrosoftService.class.getName());

	private final String webhookUrl;
	private final String jenkinsUrl;

	public RspecMicrosoftService(String webhookUrl) {
		this.webhookUrl = webhookUrl;
		this.jenkinsUrl = JenkinsLocationConfiguration.get().getUrl();
	}

	public void sendRspecReportToMicrosoft(Run<?,?> build, FilePath workspace, String json, String extra, boolean hideSuccessfulResults) {
		LOG.info("Posting rspec reports to 365 for '" + build.getParent().getDisplayName() + "'");
		LOG.info("Rspec reports are in '" + workspace + "'");
		
		LOG.info("Webhook: " + webhookUrl);

		String userName = build.getCauses().get(0).getShortDescription().replaceAll("Started by ", "");;
		String duration = build.getDurationString().replaceAll(" and counting", "");

		JsonElement jsonElement = getResultFileAsJsonElement(workspace, json);
		MicrosoftClient client = new MicrosoftClient(webhookUrl, jenkinsUrl, hideSuccessfulResults);
		client.postToMicrosoft(jsonElement, build.getParent().getDisplayName(), build.getNumber(), extra, userName, duration);
	}

	private JsonElement getResultFileAsJsonElement(FilePath workspace, String json) {		
		final Gson gson = new Gson();
		try {
			final FilePath jsonPath = new FilePath(workspace, json);
			LOG.info("file path: " + jsonPath);
			
			final JsonReader jsonReader = new JsonReader(new InputStreamReader(jsonPath.read()));
			return gson.fromJson(jsonReader, JsonElement.class);
		} catch (IOException e) {
			LOG.severe("Exception occurred while reading test results: " + e);
			throw new RuntimeException("Exception occurred while reading test results", e);
		} catch (InterruptedException e) {
			LOG.severe("Exception occurred while reading test results: " + e);
			throw new RuntimeException("Exception occurred while reading test results", e);
		}
	}
}
