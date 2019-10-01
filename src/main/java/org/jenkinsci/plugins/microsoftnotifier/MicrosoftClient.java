package org.jenkinsci.plugins.microsoftnotifier;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MicrosoftClient {

	private static final Logger LOG = Logger.getLogger(MicrosoftClient.class.getName());

	private static final String ENCODING = "UTF-8";
	private static final String CONTENT_TYPE = "application/json";

	private final String webhookUrl;
	private final String jenkinsUrl;
	private final boolean hideSuccessfulResults;

	public MicrosoftClient(String webhookUrl, String jenkinsUrl, boolean hideSuccessfulResults) {
		this.webhookUrl = webhookUrl;
		this.jenkinsUrl = jenkinsUrl;
		this.hideSuccessfulResults = hideSuccessfulResults;
	}

	public void postToMicrosoft(JsonElement results, final String jobName, final int buildNumber, final String extra, final String userName, final String duration) {
		LOG.info("Publishing test report to 365");
		CucumberResult result = results == null ? dummyResults() : processResults(results);
		String json = result.toMicrosoftMessage(jobName, buildNumber, jenkinsUrl, extra, userName, duration);
		postToMicrosoft(json);
	}

	private CucumberResult dummyResults() {
		return new CucumberResult(Arrays.asList(new FeatureResult("feature/web/dummy_test.feature", 100, 10, 0, 0)),1, 100, 0, 0, 0);
	}

	
	private void postToMicrosoft(String json) {
		LOG.fine("Json being posted: " + json);
		StringRequestEntity requestEntity = getStringRequestEntity(json);
		PostMethod postMethod = new PostMethod(webhookUrl);
		postMethod.setRequestEntity(requestEntity);
		postToMicrosoft(postMethod);
	}

	private void postToMicrosoft(PostMethod postMethod) {
		HttpClient http = new HttpClient();
		try {
			int status = http.executeMethod(postMethod);
			if (status != 200) {
				throw new RuntimeException("Received HTTP Status code [" + status + "] while posting to 365");
			}
		} catch (IOException e) {
			throw new RuntimeException("Message could not be posted", e);
		}
	}
	
	public CucumberResult processResults(JsonElement resultElement) {
		int totalScenarios = 0;
		int passPercent = 0;
		int totaFailedScenarios = 0;
		int totalPendingScenarios = 0;
		List<FeatureResult> results = new ArrayList<FeatureResult>();
		JsonArray features = resultElement.getAsJsonArray();
		for (JsonElement featureElement : features) {
			JsonObject feature = featureElement.getAsJsonObject();
			JsonArray elements = feature.get("elements").getAsJsonArray();
			int scenariosTotal = elements.size();
			int failed = 0;
			int pending = 0;
			for (JsonElement scenarioElement : elements) {
				JsonObject scenario = scenarioElement.getAsJsonObject();
				JsonArray steps = scenario.get("steps").getAsJsonArray();
				for (JsonElement stepElement : steps) {
					JsonObject step = stepElement.getAsJsonObject();
					String result = step.get("result").getAsJsonObject().get("status").getAsString();
					if (!result.equals("passed")) {
						if (result.equals("failed")) {
							failed = failed + 1;
							totaFailedScenarios = totaFailedScenarios + 1;
						} else {
							pending = pending + 1;
							totalPendingScenarios = totalPendingScenarios + 1;
						}
						break;
					}
				}
			}
			totalScenarios = totalScenarios + scenariosTotal;
			final int scenarioPassPercent = Math.round(((scenariosTotal - failed) * 100) / scenariosTotal);
			if (scenarioPassPercent != 100 || !hideSuccessfulResults) {
				results.add(new FeatureResult(feature.get("uri").getAsString(), scenarioPassPercent, (scenariosTotal - failed - pending), failed, pending));
			}
		}
		passPercent = Math.round(((totalScenarios - totaFailedScenarios) * 100) / totalScenarios);
		return new CucumberResult(results, totalScenarios, passPercent, (totalScenarios - totaFailedScenarios - totalPendingScenarios), totaFailedScenarios, totalPendingScenarios);
	}

	private StringRequestEntity getStringRequestEntity(String json) {
		try {
			return new StringRequestEntity(json, CONTENT_TYPE, ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(ENCODING + " encoding is not supported with [" + json + "]", e);
		}
	}
}
