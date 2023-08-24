package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class DataClientDeobfuscator {

	private static DataClientDeobfuscator instance = null;
	SootClass dClientClass;
	HashMap<String, String> obfuscationMap;
	HashMap<String, String> obfuscationMapCallback;

	DeofuscatorUtil deobUtil;

	public HashMap<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	public HashMap<String, String> getObfuscationCallbackMap() {
		return obfuscationMapCallback;
	}

	public static DataClientDeobfuscator getInstance() {
		if (instance == null)
			instance = new DataClientDeobfuscator();
		return instance;
	}

	private DataClientDeobfuscator() {
		this.dClientClass = Scene.v().getSootClass(ExtraTypes.DATA_CLIENT);
		obfuscationMap = new HashMap<String, String>();
		obfuscationMapCallback = new HashMap<String, String>();
		deobUtil = DeofuscatorUtil.getInstance();
	}

	public boolean deobfuscateClass() {
		boolean obfuscated = false;
		try {
			List<Type> params = new ArrayList<>();
			SootMethod sm = dClientClass.getMethodByNameUnsafe("putDataItem");
			if (sm == null) {
				Type pdr = Scene.v().getTypeUnsafe(ExtraTypes.PUT_DATA_REQUEST);
				params.add(pdr);
				List<SootMethod> result = deobUtil.searchMethodsByParams(dClientClass, params);
				if (result.size() == 1) {
					obfuscated = true;
					obfuscationMap.put(result.get(0).getSubSignature(), "putDataItem");
					result.get(0).setName("putDataItem");
				}
			}

			SootClass child = Scene.v().getSootClass(ExtraTypes.ON_DATA_CHANGED_LISTENER);
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
