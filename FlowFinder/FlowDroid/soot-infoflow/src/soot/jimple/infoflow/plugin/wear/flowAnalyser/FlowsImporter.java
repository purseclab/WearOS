package soot.jimple.infoflow.plugin.wear.flowAnalyser;

import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import soot.jimple.infoflow.plugin.wear.extras.Keys;

public class FlowsImporter {

	private static FlowsImporter importInstance;

	private FlowsImporter() {
	};

	public static FlowsImporter getInstance() {
		if (importInstance == null)
			importInstance = new FlowsImporter();
		return importInstance;

	}

	public Object readJsonFile(String filename) throws IOException, ParseException {
		FileReader reader = new FileReader(filename);
		JSONParser jsonParser = new JSONParser();
		return jsonParser.parse(reader);

	}

	@SuppressWarnings("unchecked")
	public JSONArray getJsonList(String filename) throws Exception {
		JSONArray jsonArray = (JSONArray) readJsonFile(filename);
		JSONArray filteredArray = new JSONArray();
		int size = jsonArray.size();
		for (int i = 0; i < size; i++) {
			JSONObject row = (JSONObject) jsonArray.get(i);
			String sourceClass = (String) row.get(Keys.SOURCE_DECLARED_CLASS);
			String sinkClass = (String) row.get(Keys.SINK_DECLARED_CLASS);
			String sourceMethod = (String) row.get(Keys.SOURCE_METHOD);
			String sinkMethod = (String) row.get(Keys.SINK_METHOD);

			if (sourceClass != null && sourceMethod != null && sourceClass.equals(ExtraTypes.DATA_MAP)
					&& sourceMethod.contains("Source"))
				filteredArray.add(row);
			else if (sinkClass != null && sinkMethod != null && sinkClass.equals(ExtraTypes.DATA_CLIENT)
					&& sinkMethod.contains("Sink"))
				filteredArray.add(row);
			else if (sinkClass != null && sinkMethod != null && sinkClass.equals(ExtraTypes.MESSAGE_CLIENT)
					&& sinkMethod.contains("sendMessage"))
				filteredArray.add(row);
			else if (sinkClass != null && sinkMethod != null && sinkClass.equals(ExtraTypes.MESSAGE_API)
					&& sinkMethod.contains("sendMessage"))
				filteredArray.add(row);
			else if (sourceClass != null && sourceMethod != null && sourceClass.equals(ExtraTypes.MESSAGE_EVENT)
					&& sourceMethod.contains("getDataSource"))
				filteredArray.add(row);
			else if (sourceClass != null && sourceMethod != null && sourceClass.equals(ExtraTypes.CHANNEL_CLIENT)
					&& (sourceMethod.contains("receiveFileSource") || sourceMethod.contains("getInputStreamSource")
							|| sourceMethod.equals("taintUri")))
				filteredArray.add(row);
			else if (sinkClass != null && sinkMethod != null && sinkClass.equals(ExtraTypes.CHANNEL_CLIENT)
					&& (sinkMethod.contains("sendFile") || (sinkMethod.contains("getOutputStream"))))
				filteredArray.add(row);

		}
		return filteredArray;

	}

}
