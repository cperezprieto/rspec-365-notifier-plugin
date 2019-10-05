package org.jenkinsci.plugins.microsoftnotifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.jenkinsci.plugins.microsoftnotifier.RspecResult;
import org.jenkinsci.plugins.microsoftnotifier.SpecResult;
import org.jenkinsci.plugins.microsoftnotifier.MicrosoftClient;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;

public class MicrosoftClientTest {

	@Test
	public void canGenerateFullFaultNoExamplesMicrosoftMessage() throws FileNotFoundException {
		JsonElement element = loadTestResultFile("failed-no-examples.json");
		assertNotNull(element);
		RspecResult result = new MicrosoftClient("https://jenkins.seosautomation.com/job/api_test/20/", "https://jenkins.seosautomation.com/", false).processResults(element);
		assertNotNull(result);
		assertNotNull(result.getSpecResults());
		assertEquals(0, result.totalExamples);
		assertEquals(0, result.totalPassedTests);
		assertEquals(0, result.totalFailedTests);
		assertEquals(0, result.totalPendingTests);
		assertEquals(3, result.failuresOutsideTests);
		assertEquals(0, result.passPercentage);
		
		String microsoftMessage = result.toMicrosoftMessage("test_job_API", 7, "https://jenkins.seosautomation.com/", null, "cperezprieto", "35 minutos");
		assertNotNull(microsoftMessage);
		assertEquals(false, microsoftMessage.contains("{\"activityTitle\":\"## Specs\""));
	}
	
	@Test
	public void canGenerateFullSuccessfulMicrosoftMessage() throws FileNotFoundException {
		JsonElement element = loadTestResultFile("successful.json");
		assertNotNull(element);
		RspecResult result = new MicrosoftClient("https://jenkins.seosautomation.com/job/api_test/20/", "https://jenkins.seosautomation.com/", false).processResults(element);
		assertNotNull(result);
		assertNotNull(result.getSpecResults());
		assertEquals(42, result.totalExamples);
		assertEquals(42, result.totalPassedTests);
		assertEquals(0, result.totalFailedTests);
		assertEquals(0, result.totalPendingTests);
		assertEquals(100, result.passPercentage);
		
		String microsoftMessage = result.toMicrosoftMessage("test_job_API", 7, "https://jenkins.seosautomation.com/", null, "cperezprieto", "35 minutos");
		assertNotNull(microsoftMessage);
	}
	
	@Test
	public void canGenerateFullFailedFailuresOutsideTestResultMicrosoftMessage() throws FileNotFoundException {
		JsonElement element = loadTestResultFile("failed-with-outside-errors.json");
		assertNotNull(element);
		RspecResult result = new MicrosoftClient("https://jenkins.seosautomation.com/job/api_test/20/", "https://jenkins.seosautomation.com/", false).processResults(element);
		assertNotNull(result);
		assertNotNull(result.getSpecResults());
		assertEquals(49, result.totalExamples);
		assertEquals(37, result.totalPassedTests);
		assertEquals(10, result.totalFailedTests);
		assertEquals(2, result.totalPendingTests);
		
		String microsoftMessage = result.toMicrosoftMessage("test_job_API", 7, "https://jenkins.seosautomation.com/", null, "cperezprieto", "35 minutos");
		assertNotNull(microsoftMessage);
	}
	
	@Test
	public void canGenerateFullSuccessfulPendingMicrosoftMessage() throws FileNotFoundException {
		JsonElement element = loadTestResultFile("pending.json");
		assertNotNull(element);
		RspecResult result = new MicrosoftClient("https://jenkins.seosautomation.com/job/api_test/20/", "https://jenkins.seosautomation.com/", false).processResults(element);
		assertNotNull(result);
		assertNotNull(result.getSpecResults());
		assertEquals(42, result.totalExamples);
		assertEquals(40, result.totalPassedTests);
		assertEquals(0, result.totalFailedTests);
		assertEquals(2, result.totalPendingTests);
		assertEquals(100, result.passPercentage);
		
		String microsoftMessage = result.toMicrosoftMessage("test_job_API", 7, "https://jenkins.seosautomation.com/", null, "cperezprieto", "35 minutos");
		assertNotNull(microsoftMessage);
	}
	
	@Test
	public void canGenerateFullFailedMicrosoftMessage() throws FileNotFoundException {
		JsonElement element = loadTestResultFile("failed.json");
		assertNotNull(element);
		RspecResult result = new MicrosoftClient("https://jenkins.seosautomation.com/job/api_test/20/", "https://jenkins.seosautomation.com/", false).processResults(element);
		assertNotNull(result);
		assertNotNull(result.getSpecResults());
		assertEquals(42, result.totalExamples);
		assertEquals(40, result.totalPassedTests);
		assertEquals(2, result.totalFailedTests);
		assertEquals(0, result.totalPendingTests);
		
		String microsoftMessage = result.toMicrosoftMessage("test_job_API", 7, "https://jenkins.seosautomation.com/", null, "cperezprieto", "35 minutos");
		assertNotNull(microsoftMessage);
	}
	
	private JsonElement loadTestResultFile(String filename) throws FileNotFoundException {
		File result = new File("src/test/resources", filename);
		assertNotNull(result);
		assertTrue(result.exists());
		return getResultFileAsJsonElement(new FileInputStream(result));
	}

	private JsonElement getResultFileAsJsonElement(InputStream stream) {
		final Gson gson = new Gson();
		final JsonReader jsonReader = new JsonReader(new InputStreamReader(stream));
		return gson.fromJson(jsonReader, JsonElement.class);
	}
}
