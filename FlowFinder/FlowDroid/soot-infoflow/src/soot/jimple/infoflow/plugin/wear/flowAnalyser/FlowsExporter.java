package soot.jimple.infoflow.plugin.wear.flowAnalyser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import soot.jimple.infoflow.plugin.wear.extras.Keys;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;

public final class FlowsExporter {

	private static FlowsExporter exporterInstance;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private Integer wearable = 0;

	private FlowsExporter() {
	};

	public static FlowsExporter getInstance() {
		if (exporterInstance == null)
			exporterInstance = new FlowsExporter();
		return exporterInstance;

	}

	@SuppressWarnings("unchecked")
	public void exportFlows(String filename, InfoflowResults results) {
		JSONArray jsonList = new JSONArray();

		int total = 0;
		try {
			if (results.getResults() != null) {

				for (ResultSinkInfo sinkInfo : results.getResults().keySet()) {

					Stmt sinkStmt = sinkInfo.getStmt();
					HashMap<String, String> SinkValues = parseSink(sinkStmt);

					if (SinkValues == null)
						continue;

					int count = 0;

					for (ResultSourceInfo source : results.getResults().get(sinkInfo)) {
						// initialise with values fetch from the sink
						count++;
						JSONObject json = new JSONObject(SinkValues);

						Stmt sourceStmt = source.getStmt();
						HashMap<String, String> SourcesValues = parseSources(sourceStmt);
						if (SourcesValues == null)
							continue;

						json.putAll(SourcesValues);
						jsonList.add(json);
					}
					total = total + count;
				}

				// System.out.println("Wearable flows: " + jsonList.size());
				GlobalStatistics.DATA_FLOWS = total;
				// wearable is global var
				GlobalStatistics.WEARABLE_DATA_FLOWS = wearable;

				if (jsonList.size() > 0)
					exportNewThread(filename, jsonList);
				// Files.write(Paths.get(filename), jsonList.toJSONString().getBytes());
				logger.info("Flows exported: " + jsonList.size());
			}
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	public static void exportNewThread(String filename, JSONArray toWrite) {

		Thread t = new Thread() {
			public void run() {
				try {
					Files.write(Paths.get(filename), toWrite.toJSONString().getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		try {
			t.start();
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	private HashMap<String, String> parseSources(Stmt sourceStmt) {
		SootMethod sourceMethod = sourceStmt.getInvokeExpr().getMethod();
		HashMap<String, String> map = new HashMap<String, String>();

		try {
			// FIXME check if we have more apis method to to filter by index
			map.put(Keys.SOURCE_INVOKE_EXPR, sourceStmt.toString());
			map.put(Keys.SOURCE_METHOD, sourceMethod.getName());
			map.put(Keys.SOURCE_DECLARED_CLASS, sourceMethod.getDeclaringClass().toString());

			String path = null;
			String key = null;
			String methodName = null;
			if (sourceMethod.getDeclaringClass().toString().equals(ExtraTypes.DATA_MAP)) {
				wearable++;
				// InvokeExpr ie = sourceStmt.getInvokeExpr();
				// int a = ie.getArgCount();
				path = sourceStmt.getInvokeExpr().getArg(1).toString();
				key = sourceStmt.getInvokeExpr().getArg(0).toString();
				map.put(Keys.SOURCE_PATH, path);
				map.put(Keys.SOURCE_KEY, key);
			} else if (sourceMethod.getDeclaringClass().toString().equals(ExtraTypes.MESSAGE_EVENT)) {
				wearable++;
				path = sourceStmt.getInvokeExpr().getArg(0).toString();
				map.put(Keys.SOURCE_PATH, path);
			} else if (sourceMethod.getDeclaringClass().toString().equals(ExtraTypes.CHANNEL_CLIENT)) {
				wearable++;
				methodName = sourceMethod.getName();
				if (methodName.equals("receiveFileSource"))
					path = sourceStmt.getInvokeExpr().getArg(3).toString();
				else if (methodName.equals("getInputStreamSource"))
					path = sourceStmt.getInvokeExpr().getArg(1).toString();
				else if (methodName.equals("taintUri"))
					path = sourceStmt.getInvokeExpr().getArg(1).toString();
				map.put(Keys.SOURCE_PATH, path);
			} else if (sourceMethod.getDeclaringClass().toString().equals(ExtraTypes.PUT_DATA_REQUEST)) {
				wearable++;
				methodName = sourceMethod.getName();
				if (methodName.equals("getAssets")) {
					key = sourceStmt.getInvokeExpr().getArg(0).toString();
					map.put(Keys.SOURCE_KEY, key);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			map = null;
		}
		return map;
	}

	private HashMap<String, String> parseSink(Stmt sinkStmt) {

		SootMethod sinkMethod = sinkStmt.getInvokeExpr().getMethod();
		HashMap<String, String> map = new HashMap<String, String>();
		String path = null;
		String key = null;
		String register = null;

		try {

			map.put(Keys.SINK_INVOKE_EXPR, sinkStmt.toString());
			map.put(Keys.SINK_DECLARED_CLASS, sinkMethod.getDeclaringClass().toString());
			map.put(Keys.SINK_METHOD, sinkMethod.getName());

			if (sinkMethod.getDeclaringClass().toString().equals(ExtraTypes.DATA_CLIENT)) {
				wearable++;
				path = sinkStmt.getInvokeExpr().getArg(0).toString();
				key = sinkStmt.getInvokeExpr().getArg(1).toString();
				register = sinkStmt.getInvokeExpr().getArg(2).toString();
				map.put(Keys.SINK_PATH, path);
				map.put(Keys.SINK_KEY, key);
				map.put(Keys.SINK_REGISTER, register);
			} else if (sinkMethod.getDeclaringClass().toString().equals(ExtraTypes.MESSAGE_CLIENT)
					&& sinkMethod.getName().equals("sendMessage")) {
				wearable++;
				path = sinkStmt.getInvokeExpr().getArg(1).toString();
				register = sinkStmt.getInvokeExpr().getArg(2).toString();
				map.put(Keys.SINK_PATH, path);
				map.put(Keys.SINK_REGISTER, register);
			} else if (sinkMethod.getDeclaringClass().toString().equals(ExtraTypes.MESSAGE_API)
					&& sinkMethod.getName().equals("sendMessage")) {
				wearable++;
				path = sinkStmt.getInvokeExpr().getArg(2).toString();
				register = sinkStmt.getInvokeExpr().getArg(3).toString();
				map.put(Keys.SINK_PATH, path);
				map.put(Keys.SINK_REGISTER, register);
			} else if (sinkMethod.getDeclaringClass().toString().equals(ExtraTypes.CHANNEL_CLIENT)) {
				wearable++;
				String methodName = sinkMethod.getName();
				if (methodName.equals("getOutputStreamSink"))
					path = sinkStmt.getInvokeExpr().getArg(1).toString();
				else if (methodName.equals("sendFileSink"))
					path = sinkStmt.getInvokeExpr().getArg(2).toString();
				else if (methodName.equals("sendFile2Sink"))
					path = sinkStmt.getInvokeExpr().getArg(4).toString();

				map.put(Keys.SINK_PATH, path);
			}
		} catch (Exception e) {
			e.printStackTrace();
			map = null;
		}

		return map;
	}

}
