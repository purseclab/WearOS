package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class WearableServiceObfuscator {

	private static WearableServiceObfuscator instance = null;
	SootClass wsClass;
	HashMap<String, String> obfuscationMap;

	DeofuscatorUtil deobUtil;

	public HashMap<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	public static WearableServiceObfuscator getInstance() {
		if (instance == null)
			instance = new WearableServiceObfuscator();
		return instance;
	}

	private WearableServiceObfuscator() {
		this.wsClass = Scene.v().getSootClass(ExtraTypes.WEARABLE_LISTENER_SERVICE);
		obfuscationMap = new HashMap<String, String>();
		deobUtil = DeofuscatorUtil.getInstance();
	}

	public boolean deobfuscateClass() {
		boolean obf = false;
		try {
			List<Type> params = new ArrayList<>();
			SootMethod sm = wsClass.getMethodByNameUnsafe("onDataChanged");
			if (sm == null) {
				Type type = Scene.v().getTypeUnsafe(ExtraTypes.DATA_EVENT_BUFFER);
				params.add(type);
				List<SootMethod> result = deobUtil.searchMethodsByParams(wsClass, params);
				if (result.size() == 1) {
					obf = true;
					obfuscationMap.put(result.get(0).getSubSignature(), "onDataChanged");
					result.get(0).setName("onDataChanged");
				}
			}
			sm = null;
			params.clear();
			sm = wsClass.getMethodByNameUnsafe("onMessageReceived");
			if (sm == null) {
				Type type = Scene.v().getTypeUnsafe(ExtraTypes.MESSAGE_EVENT);
				params.add(type);
				List<SootMethod> result = deobUtil.searchMethodsByParams(wsClass, params);
				if (result.size() == 1) {
					obf = true;
					obfuscationMap.put(result.get(0).getSubSignature(), "onMessageReceived");
					result.get(0).setName("onMessageReceived");
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return obf;
	}

}
