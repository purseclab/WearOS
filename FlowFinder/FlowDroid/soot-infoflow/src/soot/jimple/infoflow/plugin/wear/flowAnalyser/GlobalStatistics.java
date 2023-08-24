package soot.jimple.infoflow.plugin.wear.flowAnalyser;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

@SuppressWarnings("unchecked")
public class GlobalStatistics {

	public static long totalDuration;
	public static long auxTime = 0;

	public static Integer TOTAL_TIME = 0;
	public static Integer DEOBFUSCATION_TIME = 0;
	public static Integer DATA_ANALYSIS_TIME = 0;
	public static Integer STRING_ANALYSIS_TIME = 0;
	public static Integer INSTRUMENTATION_TIME = 0;
	public static Integer WEARABLE_APIS = 0;
	public static Integer COMPONENTS_INSTRUMENTED = 0;
	public static Integer DATA_FLOWS = 0;
	public static Integer WEARABLE_DATA_FLOWS = 0;

	public static Integer OBFUSCATED = 0;
	public static Integer HEAVY_OBFUSCATION = 0;
	public static Integer NO_INSTRUMENTATION = 0;
	public static Integer PREPROCESSING = 0;

	public static Map<String, Integer> APIS_STATISTIC = new HashMap<>();

	public static JSONObject GLOBAL_STATISTICS;

	public static JSONObject CURRENT_STATISTICS;

	public static JSONObject readStatistics(String filePath) {
		JSONObject stats = null;
		try {
			FileReader reader = new FileReader(filePath);
			JSONParser jsonParser = new JSONParser();
			Object obj = jsonParser.parse(reader);
			stats = (JSONObject) obj;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return stats;
	}

	private static void updateValue(JSONObject global, JSONObject obj, String key) {
		String gValue = null;
		Integer gIntValue = null;

		Object o = global.get(key);
		if (o == null)
			return;
		if (o instanceof String) {
			gValue = (String) global.get(key);
			gIntValue = Integer.parseInt(gValue);
		} else if (o instanceof Integer) {
			gIntValue = (int) global.get(key);

		} else if (o instanceof Long) {
			gIntValue = ((Long) global.get(key)).intValue();

		}

		Object current = obj.get(key);
		if (current == null)
			return;

		Integer oIntValue = ((Long) current).intValue();

		global.put(key, gIntValue + oIntValue);

	}

	public static void addToGlobalStatistics(JSONObject global, JSONObject obj) {
		updateValue(global, obj, ExtraTypes.DATA_CLIENT);
		updateValue(global, obj, ExtraTypes.DATA_API);
		updateValue(global, obj, ExtraTypes.DATA_MAP);
		updateValue(global, obj, ExtraTypes.MESSAGE_API);
		updateValue(global, obj, ExtraTypes.MESSAGE_CLIENT);
		updateValue(global, obj, ExtraTypes.MESSAGE_EVENT);
		updateValue(global, obj, ExtraTypes.CHANNEL_CLIENT);
		updateValue(global, obj, "WEARABLE_APIS");
		updateValue(global, obj, "COMPONENTS_INSTRUMENTED");
		updateValue(global, obj, "TOTAL_TIME");
		updateValue(global, obj, "DATA_ANALYSIS_TIME");
		updateValue(global, obj, "STRING_ANALYSIS_TIME");
		updateValue(global, obj, "INSTRUMENTATION_TIME");
		updateValue(global, obj, "DATA_FLOWS");
		updateValue(global, obj, "WEARABLE_DATA_FLOWS");

		updateValue(global, obj, "DEOBFUSCATION_TIME");
		updateValue(global, obj, "OBFUSCATED");
		updateValue(global, obj, "HEAVY_OBFUSCATION");
		updateValue(global, obj, "PREPROCESSING");
		updateValue(global, obj, "NO_INSTRUMENTATION");

	}

	public static void exportError(String filePath) {
		JSONObject obj = new JSONObject();
		obj.put("HEAVY_OBFUSCATION", HEAVY_OBFUSCATION);
		obj.put("NO_INSTRUMENTATION", NO_INSTRUMENTATION);
		obj.put("OBFUSCATED", OBFUSCATED);
		writeStatisticsNewThread(obj, filePath);

	}

	public static void exportSingleStatistics(String filePath) {
		CURRENT_STATISTICS = new JSONObject(APIS_STATISTIC);

		CURRENT_STATISTICS.put("PREPROCESSING", PREPROCESSING);
		CURRENT_STATISTICS.put("DEOBFUSCATION_TIME", DEOBFUSCATION_TIME);
		CURRENT_STATISTICS.put("DATA_ANALYSIS_TIME", DATA_ANALYSIS_TIME);
		CURRENT_STATISTICS.put("STRING_ANALYSIS_TIME", STRING_ANALYSIS_TIME);
		CURRENT_STATISTICS.put("INSTRUMENTATION_TIME", INSTRUMENTATION_TIME);

		TOTAL_TIME = PREPROCESSING + DEOBFUSCATION_TIME + STRING_ANALYSIS_TIME + INSTRUMENTATION_TIME
				+ DATA_ANALYSIS_TIME;
		CURRENT_STATISTICS.put("TOTAL_TIME", TOTAL_TIME);

		CURRENT_STATISTICS.put("WEARABLE_APIS", WEARABLE_APIS);
		CURRENT_STATISTICS.put("COMPONENTS_INSTRUMENTED", COMPONENTS_INSTRUMENTED);
		CURRENT_STATISTICS.put("DATA_FLOWS", DATA_FLOWS);
		CURRENT_STATISTICS.put("WEARABLE_DATA_FLOWS", WEARABLE_DATA_FLOWS);
		CURRENT_STATISTICS.put("OBFUSCATED", OBFUSCATED);
		CURRENT_STATISTICS.put("HEAVY_OBFUSCATION", HEAVY_OBFUSCATION);
		CURRENT_STATISTICS.put("NO_INSTRUMENTATION", NO_INSTRUMENTATION);

		writeStatisticsNewThread(CURRENT_STATISTICS, filePath);

	}

	public static void setTotalFlows(JSONObject global, int newFlows) {
		// String gValue = (String) global.get("TOTAL_MATCHED_FLOWS");
		// Integer gIntValue = Integer.parseInt(gValue);
		Integer gIntValue = ((Long) global.get("TOTAL_MATCHED_FLOWS")).intValue();
		global.put("TOTAL_MATCHED_FLOWS", gIntValue + newFlows);
	}

	public static void setFlowDirections(JSONObject global, int mobWear, int wearMobile) {
		Integer gIntValue = ((Long) global.get("MOBILE_WEAR")).intValue();
		global.put("MOBILE_WEAR", gIntValue + mobWear);

		Integer gwIntValue = ((Long) global.get("WEAR_MOBILE")).intValue();
		global.put("WEAR_MOBILE", gwIntValue + wearMobile);

	}

	public static void setLastApp(JSONObject global, String appName) {
		global.put("LAST_APP", appName);
		// String gValue = (String) global.get("TOTAL_APPS");
		Integer gIntValue = ((Long) global.get("TOTAL_APPS")).intValue();
		global.put("TOTAL_APPS", gIntValue + 1);
	}

	public static void writeMatchedFlows(String line, String pathToWrite) {
		try {
			Files.write(Paths.get(pathToWrite), line.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeStatisticsNewThread(JSONObject obj, String pathToWrite) {

		Thread t = new Thread() {
			public void run() {
				try {
					Files.write(Paths.get(pathToWrite), obj.toJSONString().getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		try {
			// t.join();
			t.start();
		} catch (Exception e) {
			e.printStackTrace();

		}

	}

	public static void writeStatistics(JSONObject obj, String pathToWrite) {
		try {
			Files.write(Paths.get(pathToWrite), obj.toJSONString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
