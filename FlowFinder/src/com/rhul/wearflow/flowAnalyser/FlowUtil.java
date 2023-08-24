package com.rhul.wearflow.flowAnalyser;

import org.json.simple.JSONObject;

import soot.jimple.infoflow.plugin.wear.extras.Keys;

public class FlowUtil {

	private static FlowUtil instance;

	private FlowUtil() {
	}

	public static FlowUtil getInstance() {
		if (instance == null)
			instance = new FlowUtil();
		return instance;
	}

	public String formatFlow(JSONObject jsonObj) {

		String sourceClass = (String) jsonObj.get(Keys.SOURCE_DECLARED_CLASS);
		String sinkClass = (String) jsonObj.get(Keys.SINK_DECLARED_CLASS);

		String sourceMethod = (String) jsonObj.get(Keys.SOURCE_METHOD);
		String sinkMethod = (String) jsonObj.get(Keys.SINK_METHOD);

		String sinkPath = (String) jsonObj.get(Keys.SINK_PATH);
		String sourcePath = (String) jsonObj.get(Keys.SOURCE_PATH);

		String sourceKey = (String) jsonObj.get(Keys.SOURCE_KEY);
		String sinkKey = (String) jsonObj.get(Keys.SINK_KEY);

		if (sourceKey == null)
			sourceKey = "";
		else
			sourceKey = " key: " + sourceKey;

		if (sinkKey == null)
			sinkKey = "";
		else
			sinkKey = " key: " + sinkKey;

		if (sourcePath == null)
			sourcePath = "";
		else
			sourcePath = " path: " + sourcePath;

		if (sinkPath == null)
			sinkPath = "";
		else
			sinkPath = " path: " + sinkPath;

		String formated = "\n\tSource-> Method: " + sourceMethod + sourcePath + sourceKey + " class: " + sourceClass
				+ "\n\t" + "Sink-> Method: " + sinkMethod + sinkPath + sinkKey + " class: " + sinkClass;

		return formated;
	}
}
