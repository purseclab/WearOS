package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class DataApiDeobfuscator {

	private static DataApiDeobfuscator instance = null;
	SootClass dApiClass;
	HashMap<String, String> obfuscationMap;
	HashMap<String, String> obfuscationMapCallback;

	DeofuscatorUtil deobUtil;

	public HashMap<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	public HashMap<String, String> getObfuscationCallbackMap() {
		return obfuscationMapCallback;
	}

	public static DataApiDeobfuscator getInstance() {
		if (instance == null)
			instance = new DataApiDeobfuscator();
		return instance;
	}

	private DataApiDeobfuscator() {
		this.dApiClass = Scene.v().getSootClass(ExtraTypes.DATA_API);
		obfuscationMap = new HashMap<String, String>();
		obfuscationMapCallback = new HashMap<String, String>();

		deobUtil = DeofuscatorUtil.getInstance();
	}

	public boolean deobfuscateClass() {
		boolean obfuscated = false;

		try {
			SootMethod sm = dApiClass.getMethodByNameUnsafe("putDataItem");
			List<Type> params = new ArrayList<>();

			if (sm == null) {
				Type gApi = Scene.v().getTypeUnsafe(ExtraTypes.GOOGLE_API_CLIENT);
				Type pdr = Scene.v().getTypeUnsafe(ExtraTypes.PUT_DATA_REQUEST);
				params.add(gApi);
				params.add(pdr);
				List<SootMethod> result = deobUtil.searchMethodsByParams(dApiClass, params);
				if (result.size() == 1) {
					obfuscated = true;
					obfuscationMap.put(result.get(0).getSubSignature(), "putDataItem");
					result.get(0).setName("putDataItem");
				}
			}
			SootClass child = Scene.v().getSootClass(ExtraTypes.DATA_LISTENER);

			if (child != null && !child.isPhantom()) {
				sm = null;
				sm = child.getMethodByNameUnsafe("onDataChanged");
				if (sm == null) {
					params.clear();
					params.add(Scene.v().getTypeUnsafe(ExtraTypes.DATA_EVENT_BUFFER));
					List<SootMethod> result = deobUtil.searchMethodsByParams(child, params);
					if (result.size() == 1) {
						obfuscated = true;
						obfuscationMapCallback.put(result.get(0).getSubSignature(), "onDataChanged");
						result.get(0).setName("onDataChanged");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obfuscated;
	}

}
