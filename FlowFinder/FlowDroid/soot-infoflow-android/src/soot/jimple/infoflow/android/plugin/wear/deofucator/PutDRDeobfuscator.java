package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class PutDRDeobfuscator {

	private static PutDRDeobfuscator instance = null;
	SootClass pdrClass;
	HashMap<String, String> obfuscationMap;
	DeofuscatorUtil deobUtil;

	public HashMap<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	public static PutDRDeobfuscator getInstance() {
		if (instance == null)
			instance = new PutDRDeobfuscator();
		return instance;
	}

	private PutDRDeobfuscator() {
		this.pdrClass = Scene.v().getSootClass(ExtraTypes.PUT_DATA_REQUEST);
		obfuscationMap = new HashMap<String, String>();
		deobUtil = DeofuscatorUtil.getInstance();
	}

	public boolean deobfuscateClass() {
		boolean obfuscated = false;

		try {
			SootMethod smc = pdrClass.getMethodByNameUnsafe("create");
			if (smc == null) {
				Type returnType = Scene.v().getTypeUnsafe(ExtraTypes.PUT_DATA_REQUEST);
				Type param = Scene.v().getTypeUnsafe(ExtraTypes.STRING_TYPE);
				List<Type> params = new ArrayList<>();
				params.add(param);
				List<SootMethod> methods = deobUtil.searchMethodsFull(pdrClass, params, returnType);
				for (SootMethod sm : methods) {

					String output = checkSignature(sm);

					if (output != null) {

						switch (output) {

						case "create":
							obfuscationMap.put(sm.getSubSignature(), "create");
							sm.setName("create");
							obfuscated = true;
							break;
						case "createWithAutoAppendedId":
							obfuscationMap.put(sm.getSubSignature(), "createWithAutoAppendedId");
							sm.setName("createWithAutoAppendedId");
							obfuscated = true;
							break;
						}
					}
				}

				params.clear();
				methods.clear();
				Type p1 = Scene.v().getTypeUnsafe(ExtraTypes.STRING_TYPE);
				Type p2 = Scene.v().getTypeUnsafe(ExtraTypes.ASSETS);
				params.add(p1);
				methods = deobUtil.searchMethodsFull(pdrClass, params, p2);
				if (methods.size() == 1) {
					obfuscated = true;
					obfuscationMap.put(methods.get(0).getSubSignature(), "getAsset");
					methods.get(0).setName("getAsset");
				}

				params.clear();
				methods.clear();
				p1 = Scene.v().getTypeUnsafe(ExtraTypes.STRING_TYPE);
				p2 = Scene.v().getTypeUnsafe(ExtraTypes.ASSETS);
				params.add(p1);
				params.add(p2);
				methods = deobUtil.searchMethodsByParams(pdrClass, params);
				if (methods.size() == 1) {
					obfuscated = true;
					obfuscationMap.put(methods.get(0).getSubSignature(), "putAsset");
					methods.get(0).setName("putAsset");
				}

				methods.clear();
				params.clear();
				Type e = Scene.v().getTypeUnsafe(ExtraTypes.DATA_ITEM);
				params.add(e);
				methods = deobUtil.searchMethodsByParams(pdrClass, params);
				if (methods.size() == 1) {
					obfuscated = true;
					obfuscationMap.put(methods.get(0).getSubSignature(), "createFromDataItem");
					methods.get(0).setName("createFromDataItem");
				}

				methods.clear();
				e = Scene.v().getTypeUnsafe(ExtraTypes.URI);
				methods = deobUtil.searchMethodByReturnType(pdrClass, e);
				if (methods.size() == 1) {
					obfuscated = true;
					obfuscationMap.put(methods.get(0).getSubSignature(), "getUri");
					methods.get(0).setName("getUri");
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obfuscated;
	}

	private String checkSignature(SootMethod sm) {
		String output = null;
		if (sm.hasActiveBody()) {
			int n = sm.getActiveBody().getLocalCount();
			if (n == 7 || n == 6 || n == 8)
				output = "createWithAutoAppendedId"; // "createWithAutoAppendedId"
			else if (n == 3) {
				Type uri = Scene.v().getTypeUnsafe(ExtraTypes.URI);
				if (deobUtil.checkLocalsType(sm, uri))
					output = "create";
				else
					output = "removeAsset";
			}
		}
		return output;
	}
}
