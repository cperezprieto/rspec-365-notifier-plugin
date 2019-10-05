package org.jenkinsci.plugins.microsoftnotifier;

import java.util.List;

import com.google.common.primitives.Chars;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class RspecResult {
	final char[] ESPECIAL_CHARACTERS = {'`', '*', '_', '{', '}', '[', ']', '(', ')', '#', '+', '-', '.', '!'};
	final String PASSED = "008000";
	final String WARNING = "FFD700";
	final String FAILED = "FF0000";
	final String IMAGE_BASE = "https://github.com/cperezprieto/rspec-365-notifier-plugin/raw/master/src/test/resources/";
	
	final List<SpecResult> specResults;
	final int passPercentage;
	final int totalExamples;
	final int totalPassedTests;
	final int totalFailedTests;
	final int totalPendingTests;
	final int failuresOutsideTests;
	String themeColour;
	String result;
	String imageResult;
	
	public RspecResult(List<SpecResult> specResults, int totalExamples, int passPercentage, int totalPassedTests, int totalFailedTests, int totalPendingTests, int failuresOutsideTests) {
		this.specResults = specResults;
		this.totalExamples = totalExamples;
		this.passPercentage = passPercentage;
		this.totalPassedTests = totalPassedTests;
		this.totalFailedTests = totalFailedTests;
		this.totalPendingTests = totalPendingTests;
		this.failuresOutsideTests = failuresOutsideTests;
	}
	
	public int getPassPercentage() {
		return this.passPercentage;
	}
	
	public int getTotalSpecs() {
		return this.specResults.size();
	}
	
	public int getTotalExamples() {
		return this.totalExamples;
	}
	
	public int getPendingTests() {
		return this.totalPendingTests;
	}
	
	public List<SpecResult> getSpecResults() {
		return this.specResults;
	}
	
	public int getFailuresOutsideTests() {
		return this.failuresOutsideTests;
	}
	
	public String toMicrosoftMessage(final String jobName,
			final int buildNumber, final String jenkinsUrl, final String extra, final String userName, final String duration) {
		final JsonObject json = new JsonObject();
		
		if (getPassPercentage() == 100) {
			if(this.totalPendingTests == 0) {
				this.themeColour = PASSED;
				this.result = "Successfully";
				this.imageResult = IMAGE_BASE + "passed.png";
			} else {
				this.themeColour = WARNING;
				this.result = "Partial successfully";
				this.imageResult = IMAGE_BASE + "warning.png";
			}
		} else {
			this.themeColour = FAILED;
			this.result = "with Errors";
			this.imageResult = IMAGE_BASE + "failed.png";
		} 
		
		if(getFailuresOutsideTests() > 0) {
			this.themeColour = FAILED;
			this.result = "with Failures outside the tests";
			this.imageResult = IMAGE_BASE + "failed.png";
		}
		
		addBaseJson(json, buildNumber, jobName, jenkinsUrl, extra);
		json.add("sections", getSections(escapeSpecialCharacters(jobName), buildNumber, jenkinsUrl, userName, duration));
		return json.toString();
	}
	
	private String escapeSpecialCharacters(String unescaped) {
		String fullEscaped = "";
		for (char c: unescaped.toCharArray()) {			
			if (Chars.contains(ESPECIAL_CHARACTERS, c)) {
				fullEscaped += "\\" + c;
			} else {
				fullEscaped += c;
			}
		}
		
		return fullEscaped;
	}

	private String getJenkinsHyperlink(final String jenkinsUrl, final String jobName, final int buildNumber) {
		StringBuilder s = new StringBuilder();
		s.append(jenkinsUrl);
		if (!jenkinsUrl.trim().endsWith("/")) {
			s.append("/");
		}
		s.append("job/");
		s.append(jobName);
		s.append("/");
		s.append(buildNumber);
		s.append("/");
		return s.toString();
	}
	
	private void addBaseJson(final JsonObject json, final int buildNumber, final String jobName, final String jenkinsUrl, final String extra) {
		json.addProperty("@type", "MessageCard");
		json.addProperty("@context", "http://schema.org/extensions");
		json.addProperty("summary", jobName + " finished " + this.result);
		json.addProperty("themeColor", this.themeColour);
	}
	
	private JsonArray getSections(final String jobName, final int buildNumber, final String jenkinsUrl, final String userName, final String duration) {
		final JsonArray sections = new JsonArray();
		sections.add(getSummarySection(jobName, buildNumber, jenkinsUrl, userName, duration));
		sections.add(getDetailSection(jobName, buildNumber, jenkinsUrl));
		return sections;
	}
	
	private JsonObject getSummarySection(final String jobName, final int buildNumber, final String jenkinsUrl, final String userName, final String duration) {		
		final String hyperLink = getJenkinsHyperlink(jenkinsUrl, jobName, buildNumber);
		final JsonObject summary = new JsonObject();
        
		summary.addProperty("activityImage", imageResult);
		summary.addProperty("activityTitle", "#**[" + jobName + "](" + hyperLink + ")**#");
		summary.addProperty("activitySubTitle", "**Percentage Passed: " + this.passPercentage + "%**");
		summary.addProperty("markdown", true);
		
		final JsonArray facts = new JsonArray();
		
		final JsonObject testSummary = new JsonObject();
		testSummary.addProperty("name", "Test Summary");
		testSummary.addProperty("value", "**Examples**: " + this.totalExamples + ", **Passed**: " + this.totalPassedTests + ", **Failed**: " +
				this.totalFailedTests + ", **Pending**: " + this.totalPendingTests + ", **Outside Failures**: " + this.failuresOutsideTests);
		
		
		final JsonObject buildSummary = new JsonObject();
		buildSummary.addProperty("name", "Build #" + buildNumber);
		buildSummary.addProperty("value", "**Started by**: " + escapeSpecialCharacters(userName) + ", **Duration**: " + duration);
		
		facts.add(testSummary);
		facts.add(buildSummary);	
		
		summary.add("facts", facts);
		
		return summary;
	}

	private JsonObject getDetailSection(final String jobName, final int buildNumber, final String jenkinsUrl) {
		final JsonObject detail = new JsonObject();
        final List<SpecResult> specs = getSpecResults();
		
        if (specs.size() > 0) {
			detail.addProperty("activityTitle", "## Specs");
			detail.addProperty("markdown", true);
			
			final JsonArray facts = new JsonArray();
			
			int count = 1;
			
			for (SpecResult spec : specs) {
				final JsonObject scenario = new JsonObject();
				scenario.addProperty("name", count);
				
				String featureColor = PASSED;
				String featureStatus = spec.getStatus();
				
				if (featureStatus == "Pending")
					featureColor = WARNING;
				
				if (featureStatus == "Failed")
					featureColor = FAILED;
				
				scenario.addProperty("value", "**<span style='color:#" + featureColor + "'>" + escapeSpecialCharacters(spec.getDisplayName()) + "</span>**");
				facts.add(scenario);
				count++;
			}
			
			detail.add("facts", facts);
		}
		
		return detail;
	}
}