package soot.jimple.infoflow.plugin.wear.flowAnalyser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import soot.jimple.infoflow.plugin.wear.extras.Keys;

public class MatchingFlowAnalysis {

	protected static Map<String, String> semanticSinkSourceTable;
	protected static Map<String, String> semanticSourceSinkTable;
	protected JSONArray mobileFlows;
	protected JSONArray wearFlows;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	List<FlowMatch> flowsMatched;
	Map<JSONObject, List<JSONObject>> mapFlows;
	int mobToWear = 0;
	int wearToMobile = 0;

	public MatchingFlowAnalysis() {
		semanticSinkSourceTable = SemanticRelation.getSinkSourceMap();
		semanticSourceSinkTable = SemanticRelation.getSourceSinkMap();
		flowsMatched = new ArrayList<FlowMatch>();
		mapFlows = new HashMap<JSONObject, List<JSONObject>>();
	}

	public List<FlowMatch> getFlowsMatched() {
		return flowsMatched;
	}

	public int getMobToWear() {
		return mobToWear;
	}

	public int getWearToMobile() {
		return wearToMobile;
	}

	/**
	 * Simple algorithm to calculate the connection between sources and sinks from a
	 * mobile-wearable app. If you need to calculate more than 1 connection for the
	 * same initial flow, don't use this function. use {@link deepFlowAnalysis},
	 * instead
	 * 
	 * @param mobileFlowFile
	 * @param wearFlowFile
	 */
	public void matchFlows(String mobileFlowFile, String wearFlowFile) {
		try {
			FlowsImporter importer = FlowsImporter.getInstance();
			mobileFlows = importer.getJsonList(mobileFlowFile);
			wearFlows = importer.getJsonList(wearFlowFile);
			logger.info(
					"Flows imported: " + "\nFrom Mobile: " + mobileFlows.size() + "\nFrom Wear: " + wearFlows.size());
			int msize = mobileFlows.size();
			int wsize = wearFlows.size();

			if (mobileFlows.size() > 0 && wearFlows.size() > 0) {

				// we search flows from mobile to wear
				logger.debug("\n\n ********* Flows from Mobile to Wear *");
				for (int i = 0; i < msize; i++) {
					List<JSONObject> tmp = new ArrayList<JSONObject>();
					JSONObject mob = (JSONObject) mobileFlows.get(i);
					logger.debug(formatFlow(mob));
					logger.debug("\n ** Flows Wear **");

					for (int j = 0; j < wsize; j++) {
						JSONObject wear = (JSONObject) wearFlows.get(j);
						logger.debug(formatFlow(wear));
						logger.debug("** next **");
						if (checkMobileWearRelation(mob, wear)) {
							flowsMatched.add(new FlowMatch(mob, wear, "mobile-wear"));
							mobToWear++;
							tmp.add(wear);
						}
					}
					mapFlows.put(mob, tmp); // contains Mobile flow with all its matched wear flows
				}

				// we search flows from wear to mobile
				logger.debug("\n\n ********* Flows from Wear to Mobile ************");

				for (int i = 0; i < wsize; i++) {
					List<JSONObject> tmp = new ArrayList<JSONObject>();
					JSONObject wear = (JSONObject) wearFlows.get(i);
					logger.debug(formatFlow(wear));
					logger.debug("\n ** Flows Mobile **");

					for (int j = 0; j < msize; j++) {
						JSONObject mob = (JSONObject) mobileFlows.get(j);
						logger.debug(formatFlow(mob));
						logger.debug("** next **");
						if (checkWearMobileSemantic(wear, mob)) {
							flowsMatched.add(new FlowMatch(mob, wear, "wear-mobile"));
							wearToMobile++;
							tmp.add(mob);
						}
					}
					mapFlows.put(wear, tmp);
				}

				logger.info("Number of simple matched flows: " + flowsMatched.size());

				List<PartialResult> depends = calculateDependencies();
				List<PartialResult> roots = getOrigins(depends);
				for (PartialResult r : roots) {
					calculatePaths(r, depends);
				}

			} else {
				logger.info("There are no flows between the mobile and the wearable app");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check weather we should join two flows. This function is direction sensitive
	 * Only considers flows FROM wear TO mobile Checks are: class, semantic-table,
	 * path, and key.
	 * 
	 * @param origin
	 * @param destination
	 * @return
	 */
	private boolean checkWearMobileSemantic(JSONObject wear, JSONObject mobile) {
		// TODO check if the string retrieve from the corresponding path. wear -> source
		// or sink
		String sourceMobileMethod = (String) mobile.get(Keys.SOURCE_METHOD);

		String sinkWearClass = (String) wear.get(Keys.SINK_DECLARED_CLASS);
		String sinkWearMethod = (String) wear.get(Keys.SINK_METHOD);

		String tmpSource = null;
		String pathMobile = null;
		String pathWear = null;
		String keyMobile = null;
		String keyWear = null;

		if (sinkWearClass.equals(ExtraTypes.DATA_CLIENT)) {
			tmpSource = semanticSinkSourceTable.get(sinkWearMethod);
			// here we check with contain because we generate 2 inStrumented sources foreach
			// original source with a "2" added in the second
			if (sourceMobileMethod.contains(tmpSource)) {
				pathMobile = (String) mobile.get(Keys.SOURCE_PATH);
				pathWear = (String) wear.get(Keys.SINK_PATH);
				if (pathMobile.equals(pathWear) || pathMobile.contains(Keys.GENERAL_PATH)
						|| pathWear.contains(Keys.GENERAL_PATH)) {
					keyMobile = (String) mobile.get(Keys.SOURCE_KEY);
					keyWear = (String) wear.get(Keys.SINK_KEY);
					if (keyMobile.equals(keyWear) || keyMobile.contains(Keys.GENERAL_KEY)
							|| keyWear.contains(Keys.GENERAL_KEY)) {
						return true;
					}
				}
			}
		} else if (sinkWearClass.equals(ExtraTypes.MESSAGE_CLIENT) || sinkWearClass.equals(ExtraTypes.MESSAGE_API)) {
			tmpSource = semanticSinkSourceTable.get(sinkWearMethod);
			if (sourceMobileMethod.equals(tmpSource)) {
				pathMobile = (String) mobile.get(Keys.SOURCE_PATH);
				pathWear = (String) wear.get(Keys.SINK_PATH);
				if (pathMobile.equals(pathWear) || pathMobile.contains(Keys.GENERAL_PATH)
						|| pathWear.contains(Keys.GENERAL_PATH))
					return true;
			}

		} else if (sinkWearClass.equals(ExtraTypes.CHANNEL_CLIENT)) {
			tmpSource = semanticSinkSourceTable.get(sinkWearMethod);
			if (sourceMobileMethod.equals(tmpSource)) {
				pathMobile = (String) mobile.get(Keys.SOURCE_PATH);
				pathWear = (String) wear.get(Keys.SINK_PATH);
				if (pathMobile.equals(pathWear) || pathMobile.contains(Keys.GENERAL_PATH)
						|| pathWear.contains(Keys.GENERAL_PATH))
					return true;
			}
		}
		return false;
	}

	/**
	 * Check weather we should join two flows. This function is direction-sensitive
	 * Only considers flows FROM mobile TO wear Checks are: class, semantic-table,
	 * path, and key. Note that the mobile side could act as a sender (sink) or
	 * receiver (source) We retrieve the corresponding relation from different
	 * tables: semanticSinkSourceTable and semanticSourceSinkTable
	 * 
	 * @param origin
	 * @param destination
	 * @return
	 */
	private boolean checkMobileWearRelation(JSONObject mobile, JSONObject wear) {
		String sourceWearMethod = (String) wear.get(Keys.SOURCE_METHOD);
		String sinkMobileClass = (String) mobile.get(Keys.SINK_DECLARED_CLASS);
		String sinkMobileMethod = (String) mobile.get(Keys.SINK_METHOD);
		String tmpSource = null;
		String pathMobile = null;
		String pathWear = null;
		String keyMobile = null;
		String keyWear = null;

		if (sinkMobileClass.equals(ExtraTypes.DATA_CLIENT)) {
			tmpSource = semanticSinkSourceTable.get(sinkMobileMethod);
			// here we check with contain because we generate 2 inStrumented sources foreach
			// original source with a "2" added in the second
			if (sourceWearMethod.contains(tmpSource)) {
				pathMobile = (String) mobile.get(Keys.SINK_PATH);
				pathWear = (String) wear.get(Keys.SOURCE_PATH);
				if (pathMobile.equals(pathWear) || pathMobile.contains(Keys.GENERAL_PATH)
						|| pathWear.contains(Keys.GENERAL_PATH)) {
					keyMobile = (String) mobile.get(Keys.SINK_KEY);
					keyWear = (String) wear.get(Keys.SOURCE_KEY);
					if (keyMobile.equals(keyWear) || keyMobile.contains(Keys.GENERAL_KEY)
							|| keyWear.contains(Keys.GENERAL_KEY)) {
						return true;
					}
				}
			}
		} else if (sinkMobileClass.equals(ExtraTypes.MESSAGE_CLIENT)
				|| sinkMobileClass.equals(ExtraTypes.MESSAGE_API)) {
			tmpSource = semanticSinkSourceTable.get(sinkMobileMethod);
			if (sourceWearMethod.equals(tmpSource)) {
				pathMobile = (String) mobile.get(Keys.SINK_PATH);
				pathWear = (String) wear.get(Keys.SOURCE_PATH);
				if (pathMobile.equals(pathWear) || pathMobile.contains(Keys.GENERAL_PATH)
						|| pathWear.contains(Keys.GENERAL_PATH))
					return true;
			}
		} else if (sinkMobileClass.equals(ExtraTypes.CHANNEL_CLIENT)) {
			tmpSource = semanticSinkSourceTable.get(sinkMobileMethod);
			if (sourceWearMethod.equals(tmpSource)) {
				pathMobile = (String) mobile.get(Keys.SINK_PATH);
				pathWear = (String) wear.get(Keys.SOURCE_PATH);
				if (pathMobile.equals(pathWear) || pathMobile.contains(Keys.GENERAL_PATH)
						|| pathWear.contains(Keys.GENERAL_PATH))
					return true;
			}
		}
		return false;
	}

	/**
	 * Run a precise analysis of flows between a mobile and wearable app. This
	 * function consider multiple connection for one initial flow. Example mob ->
	 * wear -> mobile -> wear
	 * 
	 * @param mobileFlowFile
	 * @param wearFlowFile
	 * @param deep
	 */
	public void deepFlowAnalysis(JSONObject entry, List<ArrayList<JSONObject>> paths) {
		Stack<JSONObject> stack = new Stack<JSONObject>();
		ArrayList<JSONObject> path = new ArrayList<JSONObject>();
		stack.push(entry);
		while (!stack.isEmpty()) {
			JSONObject current = stack.pop();
			path.add(current);
			List<JSONObject> childs = mapFlows.get(current);
			if (childs.size() == 0 && path.size() > 1) {
				paths.add(path);
				if (stack.isEmpty())
					continue;
				JSONObject next = stack.firstElement();
				int index = path.size() - 1;
				boolean deletePrevious = true;
				while (deletePrevious) {
					if (!mapFlows.get(path.get(index)).contains(next))
						path.remove(index);
				}
			} else {
				for (JSONObject child : childs)
					stack.push(child);
			}
		}

		return;
	}

	public List<PartialResult> calculateDependencies() {
		List<PartialResult> dependencies = new ArrayList<PartialResult>();
		for (int i = 0; i < flowsMatched.size(); i++) {

			FlowMatch pivot = flowsMatched.get(i);
			logger.info(pivot.toString());
			JSONObject pivotOrigin = getOriginOrDestination(pivot, "origin");
			JSONObject pivotDest = getOriginOrDestination(pivot, "destination");
			PartialResult tmp = new PartialResult(pivot);

			for (int j = 0; j < flowsMatched.size(); j++) {
				if (i != j) {
					FlowMatch candidate = flowsMatched.get(j);
					logger.info(candidate.toString());
					JSONObject candidateOrigin = getOriginOrDestination(candidate, "origin");
					JSONObject candidateDest = getOriginOrDestination(candidate, "destination");
					if (pivotOrigin.equals(candidateDest))
						tmp.predecesors.add(candidate);
					else if (pivotDest.equals(candidateOrigin))
						tmp.succesors.add(candidate);

					dependencies.add(tmp);
				}
			}
		}
		return dependencies;
	}

	public List<PartialResult> getOrigins(List<PartialResult> dependencies) {
		List<PartialResult> result = new ArrayList<PartialResult>();
		for (PartialResult r : dependencies) {
			if (r.predecesors.size() == 0)
				result.add(r);
		}

		return result;
	}

	public PartialResult getPartialResult(FlowMatch target, List<PartialResult> dependencies) {
		PartialResult result = null;
		for (PartialResult res : dependencies) {
			if (res.pivot.equals(target)) {
				result = res;
				break;
			}
		}
		return result;
	}

	public List<PartialResult> getListPartialResult(List<FlowMatch> targets, List<PartialResult> dependencies) {
		List<PartialResult> results = new ArrayList<PartialResult>();
		for (FlowMatch fm : targets) {
			results.add(getPartialResult(fm, dependencies));
		}
		return results;
	}

	public void calculatePaths(PartialResult origin, List<PartialResult> dependencies) {
		List<FlowMatch> paths = new ArrayList<FlowMatch>();
		List<FlowMatch> visited = new ArrayList<FlowMatch>();
		PartialResult current = origin;
		Stack<PartialResult> stack = new Stack<PartialResult>();
		stack.push(current);
		boolean newPath = false;
		while (!stack.isEmpty()) {
			current = stack.pop();
			FlowMatch pivot = current.pivot;

			if (newPath) {
				removeFromPath(current, paths);
				newPath = false;
			}

			if (!visited.contains(pivot)) {
				paths.add(pivot);
				visited.add(pivot);
			}
			List<FlowMatch> succesors = current.succesors;
			if (succesors.size() == 0) {
				System.out.println("************* New PATH ***********");
				for (FlowMatch fm : paths) {
					System.out.println(fm.toString());
				}
				newPath = true;
				continue;
			}
			List<PartialResult> succResults = getListPartialResult(succesors, dependencies);
			for (PartialResult r : succResults) {
				if (!visited.contains(r.pivot))
					stack.push(r);
			}

		}
	}

	/**
	 * Removes old entries from the path according to the current result
	 * 
	 * @param current
	 * @param paths
	 */
	private void removeFromPath(PartialResult current, List<FlowMatch> paths) {
		List<FlowMatch> preds = current.predecesors;
		int index = -1;
		for (FlowMatch fm : preds) {
			if (paths.contains(fm)) {
				index = paths.indexOf(fm);
				break;
			}
		}
		for (int i = index; i < paths.size() - 1; i++)
			paths.remove(index);
	}

	public JSONObject getOriginOrDestination(FlowMatch match, String direction) {
		JSONObject object = null;
		if (match.getDirection().equals("mobile-wear")) {
			if (direction.equals("origin"))
				object = match.getMobile();
			else if (direction.equals("destination"))
				object = match.getWear();
		} else {
			// direction == "wear-mobile"
			if (direction.equals("destination"))
				object = match.getMobile();
			else if (direction.equals("origin"))
				object = match.getWear();

		}
		return object;
	}

	public void printMatchedFlows() {
		for (FlowMatch flow : flowsMatched) {
			logger.info(flow.toString());

		}

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
			sourceKey = "null";

		if (sinkKey == null)
			sinkKey = "null";
		String formated = "\nSource-> Method:" + sourceMethod + " path: " + sourcePath + " key:" + sourceKey
				+ " Class: " + sourceClass + "\n" + "Sink-> Method:" + sinkMethod + " path: " + sinkPath + " key:"
				+ sinkKey + " Class: " + sinkClass;

		return formated;
	}

	class PartialResult {
		FlowMatch pivot;
		List<FlowMatch> predecesors;
		List<FlowMatch> succesors;

		public PartialResult(FlowMatch pivot) {
			this.pivot = pivot;
			this.predecesors = new ArrayList<FlowMatch>();
			this.succesors = new ArrayList<FlowMatch>();
		}

	}
}
