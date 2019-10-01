package org.jenkinsci.plugins.microsoftnotifier;

public class SpecResult {
	final String uri;
	final int passPercentage;
	final int testsPassed;
	final int testsFailed;
	final int testsPending;

	public SpecResult(String uri, int passPercentage, int testsPassed, int testsFailed, int testsPending) {
		this.uri = uri;
		this.passPercentage = passPercentage;
		this.testsPassed = testsPassed;
		this.testsFailed = testsFailed;
		this.testsPending = testsPending;
	}

	public String toString() {
		return this.uri + "=" + this.passPercentage;
	}
	
	public String getName() {
		return this.uri;
	}
	
	public String getSpecUri() {
		return this.uri.replace(".feature", "-feature") + ".html";
	}
	
	public String getDisplayName() {
		return this.uri;
	}
	
	public int getPassPercentage() {
		return this.passPercentage;
	}
	
	public String getStatus() {
		if (this.passPercentage == 100) {
			if (this.testsPending == 0) {
				return "Passed";
			} else {
				return "Pending";
			}
		} else {
			return "Failed";
		}
	}
}