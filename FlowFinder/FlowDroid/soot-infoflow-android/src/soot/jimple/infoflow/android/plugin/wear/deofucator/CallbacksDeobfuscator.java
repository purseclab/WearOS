package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class CallbacksDeobfuscator {

	private static CallbacksDeobfuscator instance = null;
	HashMap<String, String> obfuscationMap;

	DeofuscatorUtil deobUtil;

	public static CallbacksDeobfuscator getInstance() {
		if (instance == null)
			instance = new CallbacksDeobfuscator();
		return instance;
	}

	private CallbacksDeobfuscator() {
		obfuscationMap = new HashMap<>();
		deobUtil = DeofuscatorUtil.getInstance();
	}

	public HashMap<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	public boolean deobfuscateGoogleApiClient() {
		boolean obf = false;
		SootClass callbacksClass = Scene.v().getSootClass(ExtraTypes.GOOGLE_API_CLIENT_CALLBACKS);
		SootMethod sm = callbacksClass.getMethodByNameUnsafe("onConnected");
		if (sm == null) {
			List<Type> params = new ArrayList<>();
			Type type = Scene.v().getTypeUnsafe(ExtraTypes.BUNDLE);
			params.add(type);
			List<SootMethod> result = deobUtil.searchMethodsByParams(callbacksClass, params);
			if (result.size() == 1) {
				obf = true;
				obfuscationMap.put(result.get(0).getSubSignature(), "onConnected");
				result.get(0).setName("onConnected");
			}
		}
		return obf;

	}

}
