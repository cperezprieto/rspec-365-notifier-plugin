package org.jenkinsci.plugins.microsoftnotifier;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.jenkinsci.plugins.microsoftnotifier.RspecResult;
import org.jenkinsci.plugins.microsoftnotifier.SpecResult;
import org.junit.Test;
import org.mortbay.util.ajax.JSON;

import com.google.gson.JsonObject;

public class RspecResultTest {
	
	@Test
	public void canGenerateBaseJson() {
		String message = successfulResult().toMicrosoftMessage("test-job", 1, "http://localhost:8080/", "Extra Content", "Carlos P", "3 min");
		assertNotNull(message);
		assertTrue(message.contains("\"@type\":\"MessageCard\","));
		assertTrue(message.contains("\"@context\":\"http://schema.org/extensions\","));
		assertTrue(message.contains("\"summary\":\"test-job finished Successfully\","));
		assertTrue(message.contains("\"themeColor\":\"008000\","));
	}

	private RspecResult successfulResult() {
		return new RspecResult(Arrays.asList(new SpecResult("features/web/dummy_test.feature", 100, 10, 0, 0)),10, 100, 10, 0, 0, 0);
	}
	
	private RspecResult successfulPendingResult() {
		return new RspecResult(Arrays.asList(new SpecResult("features/web/dummy_test.feature", 100, 0, 0, 1)), 10, 100, 9, 0, 1, 0);
	}
	
	private RspecResult successfulFailuresOutsideTestResult() {
		return new RspecResult(Arrays.asList(new SpecResult("features/web/dummy_test.feature", 100, 0, 0, 1)), 10, 100, 9, 0, 1, 1);
	}
	
	private RspecResult failTestResult() {
		return new RspecResult(Arrays.asList(new SpecResult("features/web/dummy_test.feature", 90, 0, 0, 1)), 10, 90, 9, 1, 0, 0);
	}
}
