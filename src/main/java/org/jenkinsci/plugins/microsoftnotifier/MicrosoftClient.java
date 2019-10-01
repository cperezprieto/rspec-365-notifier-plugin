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
		RspecResult result = results == null ? dummyResults() : processResults(results);
		String json = result.toMicrosoftMessage(jobName, buildNumber, jenkinsUrl, extra, userName, duration);
		postToMicrosoft(json);
	}

	private RspecResult dummyResults() {
		return new RspecResult(Arrays.asList(new SpecResult("feature/web/dummy_test.feature", 100, 10, 0, 0)),1, 100, 0, 0, 0, 0);
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
	
	public RspecResult processResults(JsonElement resultElement) {
		int totalExamples = 0;
		int totalPassPercent = 0;
		int totaFailedScenarios = 0;
		int totalPendingScenarios = 0;
		int totalFailuresOutsideExample = 0;
		
		JsonObject summary = resultElement.getAsJsonObject().get("summary").getAsJsonObject();
		totalExamples = summary.get("example_count").getAsInt();
		totaFailedScenarios = summary.get("failure_count").getAsInt();
		totalPendingScenarios = summary.get("pending_count").getAsInt();
		totalFailuresOutsideExample = summary.get("errors_outside_of_examples_count").getAsInt();
		totalPassPercent = Math.round(((totalExamples - totaFailedScenarios) * 100) / totalExamples);
		
		ArrayList<SpecResult> results = new ArrayList<SpecResult>();

		SpecGroup specGroup = new SpecGroup();
		
		JsonArray examples = resultElement.getAsJsonObject().get("examples").getAsJsonArray();		
		for (JsonElement exampleElement : examples) {
			JsonObject exampleObject = exampleElement.getAsJsonObject();
			Example example = new Example();			
			example.uri = exampleObject.get("file_path").getAsString().replaceFirst("./", "");
			example.totalTests = 1;
			example.testsPassed = 0;
			example.testsFailed = 0;
			example.testsPending = 0;
			
			String status = exampleObject.get("status").getAsString();
			
			if (status.toLowerCase().equals("passed"))
				example.testsPassed++;
			if (status.toLowerCase().equals("failed"))
				example.testsFailed++;
			if (status.toLowerCase().equals("pending"))
				example.testsPending++;
			
			specGroup.addExample(example);
		}
		
		for (Example example : specGroup.getGroupedExamples()) {
			results.add(new SpecResult(example.uri, Math.round(((example.totalTests - example.testsFailed) * 100) / example.totalTests), example.testsPassed, example.testsFailed, example.testsPending));
		}
		
		totalPassPercent = Math.round(((totalExamples - totaFailedScenarios) * 100) / totalExamples);
		return new RspecResult(results, totalExamples, totalPassPercent, (totalExamples - totaFailedScenarios - totalPendingScenarios), totaFailedScenarios, totalPendingScenarios, totalFailuresOutsideExample);
	}

	private StringRequestEntity getStringRequestEntity(String json) {
		try {
			return new StringRequestEntity(json, CONTENT_TYPE, ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(ENCODING + " encoding is not supported with [" + json + "]", e);
		}
	}
}

class Example {
	String uri;
	int totalTests;
	int testsPassed;
	int testsFailed;
	int testsPending;
}

class SpecGroup {
	private ArrayList<Example> exampleList = new ArrayList<Example>();
	
	public ArrayList<Example> getGroupedExamples() {
		return exampleList;		
	}
	
	public void addExample(Example example) {
		FoundIndex foundElement = checkUriIndex(example.uri);
		
		if (foundElement.exists == true) {
			Example previousExample = exampleList.get(foundElement.index);
			Example updatedExample = new Example();
			updatedExample.uri = previousExample.uri;
			updatedExample.totalTests = previousExample.totalTests + 1;
			updatedExample.testsPassed = previousExample.testsPassed + example.testsPassed;
			updatedExample.testsFailed = previousExample.testsFailed + example.testsFailed;
			updatedExample.testsPending = previousExample.testsPending + example.testsPending;
			
			exampleList.set(foundElement.index, updatedExample);
		} else {
			exampleList.add(example);
		}
	}
	
	private FoundIndex checkUriIndex(String uri) {
		int i = 0;
		for (Example example : exampleList) {
			if (example.uri.equals(uri)) {
				return new FoundIndex(true, i);
			}
			i++;
		}
		return new FoundIndex(false, 0);
	}
}

class FoundIndex {
	final boolean exists;
	final int index;
	
	public FoundIndex(boolean exists, int index) {
		this.exists = exists;
		this.index = index;
	}
}
