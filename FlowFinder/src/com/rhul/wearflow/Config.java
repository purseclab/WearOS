package com.rhul.wearflow;

public class Config {

	public static Config instance = null;
	private String androidJars;
	private String mobileApkPath;
	private String apkName;
	private String sourceSinksFilePath;
	private String taintWrapperFilePath;
	private String stringFilePath;
	private String inputMatching;

	public static Config getInstance() {
		if (instance == null)
			instance = new Config();
		return instance;
	}

	private Config() {
	}

	public String getAndroidJars() {
		return androidJars;
	}

	public void setAndroidJars(String androidJars) {
		this.androidJars = androidJars;
	}


	public String getApkName() {
		return apkName;
	}

	public void setApkName(String apkName) {
		this.apkName = apkName;
	}


	public String getSourceSinksFilePath() {
		return sourceSinksFilePath;
	}

	public void setSourceSinksFilePath(String sourceSinksFilePath) {
		this.sourceSinksFilePath = sourceSinksFilePath;
	}

	public String getTaintWrapperFilePath() {
		return taintWrapperFilePath;
	}

	public void setTaintWrapperFilePath(String taintWrapperFilePath) {
		this.taintWrapperFilePath = taintWrapperFilePath;
	}

	public String getStringFilePath() {
		return stringFilePath;
	}

	public void setStringFilePath(String stringFilePath) {
		this.stringFilePath = stringFilePath;
	}

	public String getMobileApkPath() {
		return mobileApkPath;
	}

	public void setMobileApkPath(String mobileApkPath) {
		this.mobileApkPath = mobileApkPath;
	}

	public String getInputMatching() {
		return inputMatching;
	}

	public void setInputMatching(String inputMatching) {
		this.inputMatching = inputMatching;
	}

}
