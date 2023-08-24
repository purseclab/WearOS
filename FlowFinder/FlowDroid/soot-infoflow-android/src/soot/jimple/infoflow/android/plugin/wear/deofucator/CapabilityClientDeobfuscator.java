package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class CapabilityClientDeobfuscator {

	private static CapabilityClientDeobfuscator instance = null;
	SootClass capabilityClientClass;
	HashMap<String, String> obfuscationMap;

	DeofuscatorUtil deobUtil;

	public HashMap<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	public static CapabilityClientDeobfuscator getInstance() {
		if (instance == null)
			instance = new CapabilityClientDeobfuscator();
		return instance;
	}

	private CapabilityClientDeobfuscator() {
		capabilityClientClass = Scene.v().getSootClass(ExtraTypes.CAPABILITY_CLIENT_ON_CHANGED_LISTENER);
		obfuscationMap = new HashMap<String, String>();
		deobUtil = DeofuscatorUtil.getInstance();
	}

	public boolean deobfuscateClass() {
		boolean obf = false;
		try {
			List<Type> params = new ArrayList<>();
			SootMethod sm = capabilityClientClass.getMethodByNameUnsafe("onCapabilityChanged");
			if (sm == null) {
				Type type = Scene.v().getTypeUnsafe(ExtraTypes.CAPABILITY_API_INFO);
				params.add(type);
				List<SootMethod> result = deobUtil.searchMethodsByParams(capabilityClientClass, params);
				if (result.size() == 1) {
					obf = true;
					obfuscationMap.put(result.get(0).getSubSignature(), "onCapabilityChanged");
					result.get(0).setName("onCapabilityChanged");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obf;
	}

}
