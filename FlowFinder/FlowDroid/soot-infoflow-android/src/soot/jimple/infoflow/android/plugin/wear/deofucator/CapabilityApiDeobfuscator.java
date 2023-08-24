package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class CapabilityApiDeobfuscator {

	private static CapabilityApiDeobfuscator instance = null;
	SootClass capabilityApiClass;
	HashMap<String, String> obfuscationMap;

	DeofuscatorUtil deobUtil;

	public HashMap<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	public static CapabilityApiDeobfuscator getInstance() {
		if (instance == null)
			instance = new CapabilityApiDeobfuscator();
		return instance;
	}

	private CapabilityApiDeobfuscator() {
		capabilityApiClass = Scene.v().getSootClass(ExtraTypes.CAPABILITY_API_LISTENER);
		obfuscationMap = new HashMap<String, String>();
		deobUtil = DeofuscatorUtil.getInstance();
	}

	public boolean deobfuscateClass() {
		boolean obf = false;
		try {
			List<Type> params = new ArrayList<>();
			SootMethod sm = capabilityApiClass.getMethodByNameUnsafe("onCapabilityChanged");
			if (sm == null) {
				Type type = Scene.v().getTypeUnsafe(ExtraTypes.CAPABILITY_API_INFO);
				params.add(type);
				List<SootMethod> result = deobUtil.searchMethodsByParams(capabilityApiClass, params);
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
