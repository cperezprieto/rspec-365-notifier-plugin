package org.jenkinsci.plugins.microsoftnotifier;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.jenkinsci.plugins.microsoftnotifier.CucumberResult;
import org.jenkinsci.plugins.microsoftnotifier.FeatureResult;
import org.junit.Test;
import org.mortbay.util.ajax.JSON;

import com.google.gson.JsonObject;

public class CucumberResultTest {
	
	@Test
	public void canGenerateBaseJson() {
		String message = successfulResult().toMicrosoftMessage("test-job", 1, "http://localhost:8080/", "Extra Content", "Carlos P", "3 min");
		assertNotNull(message);
		assertTrue(message.contains("\"@type\":\"MessageCard\","));
		assertTrue(message.contains("\"@context\":\"http://schema.org/extensions\","));
		assertTrue(message.contains("\"summary\":\"test-job finished Successfully\","));
		assertTrue(message.contains("\"themeColor\":\"008000\","));
	}
/*	
	@Test
	public void canGenerateHeaderWithExtraInformation() {
		String header = successfulResult().toHeader("test-job", 1, "http://localhost:8080/", "Extra Content", 0);
		assertNotNull(header);
		assertTrue(header.contains("Extra Content"));
		assertTrue(header.contains("Features: 1"));
		assertTrue(header.contains("Scenarios: 1"));
		assertTrue(header.contains("Build: <http://localhost:8080/job/test-job/1/cucumber-html-reports/|1>"));
	}
	
	@Test
	public void canGenerateHeaderWithPendingInformation() {
		String header = successfulPendingResult().toHeader("test-job", 1, "http://localhost:8080/", "Extra Content", 1);
		assertNotNull(header);
		assertTrue(header.contains("Extra Content"));
		assertTrue(header.contains("Features: 1"));
		assertTrue(header.contains("Scenarios: 1"));
		assertTrue(header.contains("Pending Tests: 1"));
		assertTrue(header.contains("Build: <http://localhost:8080/job/test-job/1/cucumber-html-reports/|1>"));
	}
	*/
	private CucumberResult successfulResult() {
		return new CucumberResult(Arrays.asList(new FeatureResult("features/web/dummy_test.feature", 100, 10, 0, 0)),10, 100, 10, 0, 0);
	}
	
	private CucumberResult successfulPendingResult() {
		return new CucumberResult(Arrays.asList(new FeatureResult("features/web/dummy_test.feature", 100, 0, 0, 1)), 10, 100, 9, 0, 1);
	}
}
